"""
Applied Flooring - CTM 纹理自动生成脚本
用于从基础材质自动生成 Athena 与 Fusion 所需的全部无缝切片贴图与拼接图。

使用方法:
    python tools/textures/generate_ctm_textures.py

算法原理与参数调整详见: tools/textures/README.md
"""

import os
import argparse
import numpy as np
from PIL import Image
from scipy.ndimage import gaussian_filter

# ==================== 默认算法参数 ====================
# 环面高斯滤波平滑核标准差 (控制噪点空间频率，越大越柔和，建议 0.4 ~ 0.6)
DEFAULT_FILTER_SIGMA = 0.5

# 环面离散拉普拉斯方程迭代求解次数
DEFAULT_LAPLACE_ITERATIONS = 300

# 伪随机数种子 (保证生成确定性与可复现性)
DEFAULT_RANDOM_SEED = 42
# =====================================================


def periodic_lowfreq_noise(size=16, filter_sigma=DEFAULT_FILTER_SIGMA, target_std=2.18, seed=DEFAULT_RANDOM_SEED):
    """
    生成在 16x16 周期环面 T^2 上无缝平铺的低频微观纹理噪声。
    使用 mode='wrap' 高斯滤波消除单像素高频杂波，同时保持周期连续性。
    """
    rng = np.random.RandomState(seed)
    base = rng.normal(0, 1.0, (size, size))
    # 环面周期模式高斯模糊：降低噪声空间频率
    blurred = gaussian_filter(base, sigma=filter_sigma, mode='wrap')
    # 重新归一化至目标标准差，确保与中心面板能量平衡
    res = (blurred - blurred.mean()) / (blurred.std() if blurred.std() > 0 else 1.0) * target_std
    return res


def generate_flooring_slices(base_img, filter_sigma=DEFAULT_FILTER_SIGMA, iterations=DEFAULT_LAPLACE_ITERATIONS):
    """
    基于基础 16x16 地板贴图生成 4 种无缝伙伴切片：
    - empty: 全连通内部无缝平铺面板
    - center: 内凹角切片（保留四角顶点）
    - h_img: 横向连通切片（去除左右竖向边框，平滑连接）
    - v_img: 纵向连通切片（去除上下横向边框，平滑连接）
    """
    arr = np.array(base_img, dtype=np.float64)
    h, w, c = arr.shape
    assert h == 16 and w == 16, f"贴图尺寸必须为 16x16，当前为 {w}x{h}"

    # 1. 环面周期拉普拉斯插值生成 empty
    # 中心 10x10 金属面板为已知区域 (Dirichlet 边界条件)
    known_mask = np.zeros((16, 16), dtype=bool)
    known_mask[3:13, 3:13] = True

    def solve_torus_laplace(channel, mask):
        u = channel.copy()
        for _ in range(iterations):
            up = np.roll(u, 1, axis=0)
            down = np.roll(u, -1, axis=0)
            left = np.roll(u, 1, axis=1)
            right = np.roll(u, -1, axis=1)
            u[~mask] = 0.25 * (up + down + left + right)[~mask]
        return u

    # 测定中心面板自然方差
    plate_std = arr[3:13, 3:13, 0].std()
    noise = periodic_lowfreq_noise(size=16, filter_sigma=filter_sigma, target_std=plate_std)

    # 求解 R 通道
    smooth_r = solve_torus_laplace(arr[:, :, 0], known_mask)
    noisy_r = smooth_r.copy()
    noisy_r[~known_mask] += noise[~known_mask]
    noisy_r = np.round(noisy_r)

    # AE2 恒定色彩偏置保持 (G - R 与 B - R 恒定)
    dg = int(np.round(arr[3:13, 3:13, 1].mean() - arr[3:13, 3:13, 0].mean()))
    db = int(np.round(arr[3:13, 3:13, 2].mean() - arr[3:13, 3:13, 0].mean()))

    empty_arr = np.zeros((16, 16, 4), dtype=np.uint8)
    empty_arr[:, :, 0] = np.clip(noisy_r, 0, 255).astype(np.uint8)
    empty_arr[:, :, 1] = np.clip(noisy_r + dg, 0, 255).astype(np.uint8)
    empty_arr[:, :, 2] = np.clip(noisy_r + db, 0, 255).astype(np.uint8)
    empty_arr[:, :, 3] = 255

    # 2. 横向切片 (h_arr): 横向全开，上下带边框
    h_arr = empty_arr.copy()
    # 行 0: 顶外边框
    h_arr[0, :, :] = arr[0, :, :]
    # 行 1: 顶内框架 (去除左右竖边交叉伪影)
    h_arr[1, :, :] = arr[1, :, :]
    h_mean1 = np.round((arr[1, 1, :3] + arr[1, 14, :3]) / 2)
    h_arr[1, 0, :3] = h_mean1
    h_arr[1, 15, :3] = h_mean1
    # 行 2: 顶倒角暗槽
    groove_top = np.round(arr[2, 3:13, :3].mean(axis=0))
    h_arr[2, :, :3] = groove_top
    # 行 13: 底倒角暗槽
    groove_bot = np.round(arr[13, 3:13, :3].mean(axis=0))
    h_arr[13, :, :3] = groove_bot
    # 行 14: 底内框架
    h_arr[14, :, :] = arr[14, :, :]
    h_mean14 = np.round((arr[14, 1, :3] + arr[14, 14, :3]) / 2)
    h_arr[14, 0, :3] = h_mean14
    h_arr[14, 15, :3] = h_mean14
    # 行 15: 底外边框
    h_arr[15, :, :] = arr[15, :, :]
    h_mean15 = np.round((arr[15, 1, :3] + arr[15, 15, :3]) / 2)
    h_arr[15, 0, :3] = h_mean15
    h_arr[15, 15, :3] = h_mean15

    # 3. 纵向切片 (v_arr): 纵向全开，左右带边框
    v_arr = empty_arr.copy()
    # 列 0: 左外边框
    v_arr[:, 0, :] = arr[:, 0, :]
    v_mean0 = np.round((arr[0, 0, :3] + arr[14, 0, :3]) / 2)
    v_arr[15, 0, :3] = v_mean0
    # 列 1: 左内框架 (去除上下横边交叉伪影)
    v_arr[:, 1, :] = arr[:, 1, :]
    v_mean1 = np.round((arr[1, 1, :3] + arr[14, 1, :3]) / 2)
    v_arr[0, 1, :3] = v_mean1
    v_arr[15, 1, :3] = v_mean1
    # 列 2: 左倒角暗槽
    groove_left = np.round(arr[3:13, 2, :3].mean(axis=0))
    v_arr[:, 2, :3] = groove_left
    # 列 13: 右倒角暗槽
    groove_right = np.round(arr[3:13, 13, :3].mean(axis=0))
    v_arr[:, 13, :3] = groove_right
    # 列 14: 右内框架
    v_arr[:, 14, :] = arr[:, 14, :]
    v_mean14 = np.round((arr[1, 14, :3] + arr[14, 14, :3]) / 2)
    v_arr[0, 14, :3] = v_mean14
    v_arr[15, 14, :3] = v_mean14
    # 列 15: 右外边框
    v_arr[:, 15, :] = arr[:, 15, :]
    v_mean15 = np.round((arr[1, 15, :3] + arr[15, 15, :3]) / 2)
    v_arr[0, 15, :3] = v_mean15
    v_arr[15, 15, :3] = v_mean15

    # 4. 内凹角切片 (center_arr): 仅保留四角顶点
    center_arr = empty_arr.copy()
    center_arr[0, 0, :] = arr[0, 0, :]
    center_arr[0, 15, :] = arr[0, 15, :]
    center_arr[15, 0, :] = arr[15, 0, :]
    center_arr[15, 15, :] = arr[15, 15, :]

    return (
        Image.fromarray(empty_arr),
        Image.fromarray(center_arr),
        Image.fromarray(h_arr),
        Image.fromarray(v_arr)
    )


def overlay_icon(slices, icon_img):
    """
    将电梯或激光连接器的功能图标 (严格限制在 3:13, 3:13) 叠加到地板切片上。
    外围区域保持 100% 相同，从而保证电梯、激光连接器与普通地板完美互连。
    """
    empty, center, h_img, v_img = slices
    icon_arr = np.array(icon_img)

    result = []
    for s in [empty, center, h_img, v_img]:
        s_arr = np.array(s)
        s_arr[3:13, 3:13, :] = icon_arr[3:13, 3:13, :]
        result.append(Image.fromarray(s_arr))
    return tuple(result)


def build_pieced_image(base, empty, v_img, h_img, center):
    """
    拼接生成 Fusion 规范所需的 80x16 贴图 (5 个 16x16 槽位: base, empty, v, h, center)。
    """
    pieced = Image.new('RGBA', (80, 16))
    pieced.paste(base, (0, 0))
    pieced.paste(empty, (16, 0))
    pieced.paste(v_img, (32, 0))
    pieced.paste(h_img, (48, 0))
    pieced.paste(center, (64, 0))
    return pieced


def main():
    parser = argparse.ArgumentParser(description="Applied Flooring CTM Texture Generator")
    parser.add_argument("--sigma", type=float, default=DEFAULT_FILTER_SIGMA,
                        help=f"高斯滤波平滑核标准差 (默认: {DEFAULT_FILTER_SIGMA})")
    parser.add_argument("--iter", type=int, default=DEFAULT_LAPLACE_ITERATIONS,
                        help=f"拉普拉斯方程迭代次数 (默认: {DEFAULT_LAPLACE_ITERATIONS})")
    args = parser.parse_args()

    # 动态获取项目根目录路径
    script_dir = os.path.dirname(os.path.abspath(__file__))
    repo_root = os.path.abspath(os.path.join(script_dir, "..", ".."))
    assets_dir = os.path.join(repo_root, "common", "src", "main", "resources", "assets", "appliedflooring")
    tex_dir = os.path.join(assets_dir, "textures", "block")
    fusion_dir = os.path.join(tex_dir, "fusion")

    assert os.path.exists(tex_dir), f"贴图目录不存在: {tex_dir}"
    os.makedirs(fusion_dir, exist_ok=True)

    colors = [
        "", "black_", "blue_", "brown_", "cyan_", "gray_", "green_", "light_blue_",
        "light_gray_", "lime_", "magenta_", "orange_", "pink_", "purple_", "red_",
        "white_", "yellow_"
    ]
    types = ["me_flooring", "me_elevator", "me_laser_connector"]
    states = [
        ("", "online"),
        ("_offline", "offline")
    ]

    athena_count = 0
    fusion_count = 0

    print(f"开始生成 CTM 贴图 (参数: sigma={args.sigma}, iter={args.iter})...")

    for c in colors:
        for suffix, state_name in states:
            # 1. 基础地板贴图
            f_base_name = f"{c}me_flooring{suffix}.png"
            f_base_path = os.path.join(tex_dir, f_base_name)
            if not os.path.exists(f_base_path):
                print(f"警告: 跳过缺失的文件 {f_base_path}")
                continue
            f_base_img = Image.open(f_base_path).convert('RGBA')

            # 生成普通地板的 4 个切片
            f_slices = generate_flooring_slices(f_base_img, filter_sigma=args.sigma, iterations=args.iter)

            # 2. 电梯方块切片 (叠加电梯中心图标)
            e_base_name = f"{c}me_elevator{suffix}.png"
            e_base_path = os.path.join(tex_dir, e_base_name)
            e_base_img = Image.open(e_base_path).convert('RGBA')
            e_slices = overlay_icon(f_slices, e_base_img)

            # 3. 激光连接器切片 (叠加激光中心图标)
            l_base_name = f"{c}me_laser_connector{suffix}.png"
            l_base_path = os.path.join(tex_dir, l_base_name)
            l_base_img = Image.open(l_base_path).convert('RGBA')
            l_slices = overlay_icon(f_slices, l_base_img)

            all_block_slices = {
                f"{c}me_flooring": (f_base_img, f_slices),
                f"{c}me_elevator": (e_base_img, e_slices),
                f"{c}me_laser_connector": (l_base_img, l_slices),
            }

            for block_name, (base_img, (empty, center, h_img, v_img)) in all_block_slices.items():
                # Athena 格式 (仅在线状态使用独立 16x16 切片文件)
                if state_name == "online":
                    empty.save(os.path.join(tex_dir, f"{block_name}_empty.png"))
                    center.save(os.path.join(tex_dir, f"{block_name}_center.png"))
                    h_img.save(os.path.join(tex_dir, f"{block_name}_h.png"))
                    v_img.save(os.path.join(tex_dir, f"{block_name}_v.png"))
                    athena_count += 4

                # Fusion 格式 (80x16 拼接图，在线与离线均支持)
                fusion_img = build_pieced_image(base_img, empty, v_img, h_img, center)
                fusion_path = os.path.join(fusion_dir, f"{block_name}_{state_name}.png")
                fusion_img.save(fusion_path)
                fusion_count += 1

    print(f"生成完成: 共生成 {athena_count} 张 Athena 切片贴图 (16x16), {fusion_count} 张 Fusion 拼接贴图 (80x16)。")


if __name__ == '__main__':
    main()
