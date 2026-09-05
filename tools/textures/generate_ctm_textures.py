"""
Applied Flooring - CTM 与全色系纹理自动生成脚本
从 6 张基础无色/原色材质自动生成 16 种 AE2 染色、Athena CTM 切片以及 Fusion 80x16 拼接图与 mcmeta。

开发者仅需维护以下 6 张基础贴图：
- me_flooring.png / me_flooring_offline.png (普通地板 在线/离线)
- me_elevator.png / me_elevator_offline.png (电梯 在线/离线)
- me_laser_connector.png / me_laser_connector_offline.png (激光连接器 在线/离线)

使用方法:
    python tools/textures/generate_ctm_textures.py
    或通过 Gradle 任务: ./gradlew generateTextures

算法原理详见: tools/textures/README.md
"""

import os
import json
import argparse
import numpy as np
from PIL import Image
from scipy.ndimage import gaussian_filter

# ==================== 默认算法参数 ====================
DEFAULT_FILTER_SIGMA = 0.5
DEFAULT_LAPLACE_ITERATIONS = 300
DEFAULT_RANDOM_SEED = 42

# AE2 官方 16 色与未染色基础材质的恒定色偏 (dr, dg, db)
COLOR_DELTAS = {
    "": (0, 0, 0),
    "black_": (-36, -28, -45),
    "blue_": (-20, 40, 133),
    "brown_": (44, 10, -47),
    "cyan_": (-34, 96, 83),
    "gray_": (6, 14, -1),
    "green_": (-30, 66, -17),
    "light_blue_": (4, 106, 150),
    "light_gray_": (74, 83, 65),
    "lime_": (30, 124, -43),
    "magenta_": (120, 10, 67),
    "orange_": (156, 53, -49),
    "pink_": (162, 66, 83),
    "purple_": (54, 0, 93),
    "red_": (117, -17, -33),
    "white_": (144, 154, 135),
    "yellow_": (172, 133, -37),
}
# =====================================================


def periodic_lowfreq_noise(size=16, filter_sigma=DEFAULT_FILTER_SIGMA, target_std=2.18, seed=DEFAULT_RANDOM_SEED):
    """
    生成在 16x16 周期环面 T^2 上无缝平铺的低频微观纹理噪声。
    """
    rng = np.random.RandomState(seed)
    base = rng.normal(0, 1.0, (size, size))
    blurred = gaussian_filter(base, sigma=filter_sigma, mode='wrap')
    res = (blurred - blurred.mean()) / (blurred.std() if blurred.std() > 0 else 1.0) * target_std
    return res


def generate_flooring_slices(base_arr, filter_sigma=DEFAULT_FILTER_SIGMA, iterations=DEFAULT_LAPLACE_ITERATIONS):
    """
    基于基础 16x16 地板贴图生成 4 种无缝伙伴切片 (empty, center, h_arr, v_arr)。
    """
    arr = base_arr.astype(np.float64)
    h, w, c = arr.shape
    assert h == 16 and w == 16, f"贴图尺寸必须为 16x16，当前为 {w}x{h}"

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

    plate_std = arr[3:13, 3:13, 0].std()
    noise = periodic_lowfreq_noise(size=16, filter_sigma=filter_sigma, target_std=plate_std)

    smooth_r = solve_torus_laplace(arr[:, :, 0], known_mask)
    noisy_r = smooth_r.copy()
    noisy_r[~known_mask] += noise[~known_mask]
    noisy_r = np.round(noisy_r)

    dg = int(np.round(arr[3:13, 3:13, 1].mean() - arr[3:13, 3:13, 0].mean()))
    db = int(np.round(arr[3:13, 3:13, 2].mean() - arr[3:13, 3:13, 0].mean()))

    empty_arr = np.zeros((16, 16, 4), dtype=np.uint8)
    empty_arr[:, :, 0] = np.clip(noisy_r, 0, 255).astype(np.uint8)
    empty_arr[:, :, 1] = np.clip(noisy_r + dg, 0, 255).astype(np.uint8)
    empty_arr[:, :, 2] = np.clip(noisy_r + db, 0, 255).astype(np.uint8)
    empty_arr[:, :, 3] = 255

    # 横向切片 (h_arr)
    h_arr = empty_arr.copy()
    h_arr[0, :, :] = arr[0, :, :]
    h_arr[1, :, :] = arr[1, :, :]
    h_mean1 = np.round((arr[1, 1, :3] + arr[1, 14, :3]) / 2)
    h_arr[1, 0, :3] = h_mean1
    h_arr[1, 15, :3] = h_mean1
    groove_top = np.round(arr[2, 3:13, :3].mean(axis=0))
    h_arr[2, :, :3] = groove_top
    groove_bot = np.round(arr[13, 3:13, :3].mean(axis=0))
    h_arr[13, :, :3] = groove_bot
    h_arr[14, :, :] = arr[14, :, :]
    h_mean14 = np.round((arr[14, 1, :3] + arr[14, 14, :3]) / 2)
    h_arr[14, 0, :3] = h_mean14
    h_arr[14, 15, :3] = h_mean14
    h_arr[15, :, :] = arr[15, :, :]
    h_mean15 = np.round((arr[15, 1, :3] + arr[15, 15, :3]) / 2)
    h_arr[15, 0, :3] = h_mean15
    h_arr[15, 15, :3] = h_mean15

    # 纵向切片 (v_arr)
    v_arr = empty_arr.copy()
    v_arr[:, 0, :] = arr[:, 0, :]
    v_mean0 = np.round((arr[0, 0, :3] + arr[14, 0, :3]) / 2)
    v_arr[15, 0, :3] = v_mean0
    v_arr[:, 1, :] = arr[:, 1, :]
    v_mean1 = np.round((arr[1, 1, :3] + arr[14, 1, :3]) / 2)
    v_arr[0, 1, :3] = v_mean1
    v_arr[15, 1, :3] = v_mean1
    groove_left = np.round(arr[3:13, 2, :3].mean(axis=0))
    v_arr[:, 2, :3] = groove_left
    groove_right = np.round(arr[3:13, 13, :3].mean(axis=0))
    v_arr[:, 13, :3] = groove_right
    v_arr[:, 14, :] = arr[:, 14, :]
    v_mean14 = np.round((arr[1, 14, :3] + arr[14, 14, :3]) / 2)
    v_arr[0, 14, :3] = v_mean14
    v_arr[15, 14, :3] = v_mean14
    v_arr[:, 15, :] = arr[:, 15, :]
    v_mean15 = np.round((arr[1, 15, :3] + arr[15, 15, :3]) / 2)
    v_arr[0, 15, :3] = v_mean15
    v_arr[15, 15, :3] = v_mean15

    # 内凹角切片 (center_arr)
    center_arr = empty_arr.copy()
    center_arr[0, 0, :] = arr[0, 0, :]
    center_arr[0, 15, :] = arr[0, 15, :]
    center_arr[15, 0, :] = arr[15, 0, :]
    center_arr[15, 15, :] = arr[15, 15, :]

    return empty_arr, center_arr, h_arr, v_arr


def apply_color_delta(arr, dr, dg, db):
    """
    对图片应用恒定 RGB 色彩偏移。
    """
    if dr == 0 and dg == 0 and db == 0:
        return arr.copy()
    out = arr.copy()
    out[:, :, 0] = np.clip(out[:, :, 0].astype(int) + dr, 0, 255).astype(np.uint8)
    out[:, :, 1] = np.clip(out[:, :, 1].astype(int) + dg, 0, 255).astype(np.uint8)
    out[:, :, 2] = np.clip(out[:, :, 2].astype(int) + db, 0, 255).astype(np.uint8)
    return out


def build_pieced_image(base, empty, v_img, h_img, center):
    """
    拼接生成 Fusion 规范所需的 80x16 贴图 (5 个 16x16 槽位: base, empty, v, h, center)。
    """
    pieced = Image.new('RGBA', (80, 16))
    pieced.paste(Image.fromarray(base), (0, 0))
    pieced.paste(Image.fromarray(empty), (16, 0))
    pieced.paste(Image.fromarray(v_img), (32, 0))
    pieced.paste(Image.fromarray(h_img), (48, 0))
    pieced.paste(Image.fromarray(center), (64, 0))
    return pieced


def main():
    parser = argparse.ArgumentParser(description="Applied Flooring CTM & Texture Generator")
    parser.add_argument("--sigma", type=float, default=DEFAULT_FILTER_SIGMA,
                        help=f"高斯滤波平滑核标准差 (默认: {DEFAULT_FILTER_SIGMA})")
    parser.add_argument("--iter", type=int, default=DEFAULT_LAPLACE_ITERATIONS,
                        help=f"拉普拉斯方程迭代次数 (默认: {DEFAULT_LAPLACE_ITERATIONS})")
    args = parser.parse_args()

    script_dir = os.path.dirname(os.path.abspath(__file__))
    repo_root = os.path.abspath(os.path.join(script_dir, "..", ".."))
    assets_dir = os.path.join(repo_root, "common", "src", "main", "resources", "assets", "appliedflooring")
    tex_dir = os.path.join(assets_dir, "textures", "block")
    fusion_dir = os.path.join(tex_dir, "fusion")

    assert os.path.exists(tex_dir), f"贴图目录不存在: {tex_dir}"
    os.makedirs(fusion_dir, exist_ok=True)

    states = [
        ("", "online"),
        ("_offline", "offline")
    ]

    print(f"开始根据 6 张主模板贴图生成全色系材质与 CTM 切片 (sigma={args.sigma}, iter={args.iter})...")

    base_count = 0
    athena_count = 0
    fusion_count = 0
    mcmeta_count = 0

    for suffix, state_name in states:
        f_base_file = os.path.join(tex_dir, f"me_flooring{suffix}.png")
        e_base_file = os.path.join(tex_dir, f"me_elevator{suffix}.png")
        l_base_file = os.path.join(tex_dir, f"me_laser_connector{suffix}.png")

        assert os.path.exists(f_base_file), f"缺失基础地板贴图: {f_base_file}"
        assert os.path.exists(e_base_file), f"缺失基础电梯贴图: {e_base_file}"
        assert os.path.exists(l_base_file), f"缺失基础激光连接器贴图: {l_base_file}"

        f_master = np.array(Image.open(f_base_file).convert('RGBA'))
        e_master = np.array(Image.open(e_base_file).convert('RGBA'))
        l_master = np.array(Image.open(l_base_file).convert('RGBA'))

        # 提取电梯与激光连接器在 3:13 内部的专属图标遮罩
        e_icon_mask = np.any(e_master != f_master, axis=2)
        l_icon_mask = np.any(l_master != f_master, axis=2)

        # 针对当前状态的地板基础贴图求解一次 Laplace CTM 切片
        f_empty, f_center, f_h, f_v = generate_flooring_slices(f_master, filter_sigma=args.sigma, iterations=args.iter)

        for c, (dr, dg, db) in COLOR_DELTAS.items():
            # 1. 普通地板各切片色偏衍生
            f_base = apply_color_delta(f_master, dr, dg, db)
            empty_f = apply_color_delta(f_empty, dr, dg, db)
            center_f = apply_color_delta(f_center, dr, dg, db)
            h_f = apply_color_delta(f_h, dr, dg, db)
            v_f = apply_color_delta(f_v, dr, dg, db)

            # 2. 电梯各切片 (地板基底 + 保留原有图标)
            e_base = f_base.copy()
            e_base[e_icon_mask] = e_master[e_icon_mask]
            empty_e = empty_f.copy()
            empty_e[e_icon_mask] = e_master[e_icon_mask]
            center_e = center_f.copy()
            center_e[e_icon_mask] = e_master[e_icon_mask]
            h_e = h_f.copy()
            h_e[e_icon_mask] = e_master[e_icon_mask]
            v_e = v_f.copy()
            v_e[e_icon_mask] = e_master[e_icon_mask]

            # 3. 激光连接器各切片 (地板基底 + 保留原有透镜图标)
            l_base = f_base.copy()
            l_base[l_icon_mask] = l_master[l_icon_mask]
            empty_l = empty_f.copy()
            empty_l[l_icon_mask] = l_master[l_icon_mask]
            center_l = center_f.copy()
            center_l[l_icon_mask] = l_master[l_icon_mask]
            h_l = h_f.copy()
            h_l[l_icon_mask] = l_master[l_icon_mask]
            v_l = v_f.copy()
            v_l[l_icon_mask] = l_master[l_icon_mask]

            all_blocks = {
                f"{c}me_flooring": (f_base, empty_f, center_f, h_f, v_f),
                f"{c}me_elevator": (e_base, empty_e, center_e, h_e, v_e),
                f"{c}me_laser_connector": (l_base, empty_l, center_l, h_l, v_l),
            }

            for block_name, (base, empty, center, h_img, v_img) in all_blocks.items():
                # 保存基础单体方块贴图
                base_path = os.path.join(tex_dir, f"{block_name}{suffix}.png")
                Image.fromarray(base).save(base_path)
                base_count += 1

                # Athena 格式 (仅在线状态声明独立的 16x16 切片文件)
                if state_name == "online":
                    Image.fromarray(empty).save(os.path.join(tex_dir, f"{block_name}_empty.png"))
                    Image.fromarray(center).save(os.path.join(tex_dir, f"{block_name}_center.png"))
                    Image.fromarray(h_img).save(os.path.join(tex_dir, f"{block_name}_h.png"))
                    Image.fromarray(v_img).save(os.path.join(tex_dir, f"{block_name}_v.png"))
                    athena_count += 4

                # Fusion 格式 (80x16 拼接图，在线与离线均支持)
                fusion_img = build_pieced_image(base, empty, v_img, h_img, center)
                fusion_img_path = os.path.join(fusion_dir, f"{block_name}_{state_name}.png")
                fusion_img.save(fusion_img_path)
                fusion_count += 1

                # Fusion .mcmeta 配置文件 (配置同色系方块互连)
                mcmeta_path = f"{fusion_img_path}.mcmeta"
                mcmeta_data = {
                    "fusion": {
                        "type": "connecting",
                        "layout": "pieced",
                        "connections": {
                            "type": "match_block",
                            "blocks": [
                                f"appliedflooring:{c}me_flooring",
                                f"appliedflooring:{c}me_elevator",
                                f"appliedflooring:{c}me_laser_connector"
                            ]
                        }
                    }
                }
                with open(mcmeta_path, "w", encoding="utf-8") as mf:
                    json.dump(mcmeta_data, mf, indent=2)
                mcmeta_count += 1

    print(f"生成完成: 共生成 {base_count} 张基础贴图, {athena_count} 张 Athena 切片, {fusion_count} 张 Fusion 80x16 贴图, {mcmeta_count} 个 mcmeta 配置文件。")


if __name__ == '__main__':
    main()
