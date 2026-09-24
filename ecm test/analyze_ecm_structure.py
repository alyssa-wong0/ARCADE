import json
from pathlib import Path

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

RUN_DIR = Path("output/output_four")
TICK = 200

CELLS_FILE = Path("output/output_four/ECM_TEST_TEST_0000_000200.CELLS.json")
LOCATIONS_FILE = Path("output/output_four/ECM_TEST_TEST_0000_000200.LOCATIONS.json")
LAYERS_FILE = Path("output/output_four/ECM_TEST_TEST_0000_000200.LAYERS.json")

with open(CELLS_FILE) as f:
    cells = json.load(f)

with open(LOCATIONS_FILE) as f:
    locations = json.load(f)

with open(LAYERS_FILE) as f:
    layers = json.load(f)

print(f"Loaded {len(cells)} cells")
print(f"Loaded {len(locations)} occupied locations")
print(f"Loaded {len(layers)} lattice locations")

cell_rows = []

for cell in cells:
    cell_rows.append({
        "id": cell["id"],
        "pop": cell["pop"],
        "state": cell["state"],
        "volume": cell["volume"],
    })

df = pd.DataFrame(cell_rows)

pop_names = {
    1: "Cancer",
    2: "CAR-T CD4",
    3: "CAR-T CD8",
}

df["population"] = df["pop"].map(pop_names)


location_rows = []

for entry in locations:
    coord = entry["coordinate"]
    x, y, z = coord[:3]

    for cell_id in entry["ids"]:
        location_rows.append({
            "id": cell_id,
            "x": x,
            "y": y,
            "z": z,
        })

loc_df = pd.DataFrame(location_rows)

df = df.merge(loc_df, on="id", how="left")


print("\n=== POPULATION COUNTS ===")
print(df["population"].value_counts())

print("\n=== STATE COUNTS ===")
print(df.groupby(["population", "state"]).size())


cancer = df[df["population"] == "Cancer"].copy()

center_x = cancer["x"].mean()
center_y = cancer["y"].mean()
center_z = cancer["z"].mean()

print("\n=== CANCER CENTER ===")
print(f"x = {center_x:.2f}")
print(f"y = {center_y:.2f}")
print(f"z = {center_z:.2f}")


df["radius"] = np.sqrt(
    (df["x"] - center_x) ** 2
    + (df["y"] - center_y) ** 2
    + (df["z"] - center_z) ** 2
)

cancer["radius"] = np.sqrt(
    (cancer["x"] - center_x) ** 2
    + (cancer["y"] - center_y) ** 2
    + (cancer["z"] - center_z) ** 2
)


print("\n=== CANCER STATE BY RADIAL DISTANCE ===")

bins = [0, 2, 4, 6, 8, 10, 12]
labels = ["0–2", "2–4", "4–6", "6–8", "8–10", "10–12"]

cancer["radial_bin"] = pd.cut(
    cancer["radius"],
    bins=bins,
    labels=labels,
    include_lowest=True
)

radial_states = pd.crosstab(
    cancer["radial_bin"],
    cancer["state"],
    normalize="index"
) * 100

print(radial_states.round(1))


plt.figure(figsize=(8, 8))

for state in cancer["state"].unique():
    subset = cancer[cancer["state"] == state]

    plt.scatter(
        subset["x"],
        subset["y"],
        label=state,
        s=70
    )

plt.scatter(
    center_x,
    center_y,
    marker="x",
    s=150,
    label="Tumor center"
)

plt.xlabel("x")
plt.ylabel("y")
plt.title("Cancer Cell States at Tick 200")
plt.legend()
plt.axis("equal")
plt.tight_layout()
plt.savefig("cancer_states.png", dpi=300)
plt.close()


plt.figure(figsize=(8, 5))

for state in cancer["state"].unique():
    subset = cancer[cancer["state"] == state]

    plt.scatter(
        subset["radius"],
        np.ones(len(subset)),
        label=state,
        s=60
    )

plt.xlabel("Distance from tumor center")
plt.title("Cancer Cell States vs Radial Distance")
plt.yticks([])
plt.legend()
plt.tight_layout()
plt.savefig("cancer_radial_states.png", dpi=300)
plt.close()


cart = df[df["population"].isin(["CAR-T CD4", "CAR-T CD8"])].copy()

plt.figure(figsize=(8, 8))

for pop in ["CAR-T CD4", "CAR-T CD8"]:
    subset = cart[cart["population"] == pop]

    plt.scatter(
        subset["x"],
        subset["y"],
        label=pop,
        s=70
    )

plt.scatter(
    center_x,
    center_y,
    marker="x",
    s=150,
    label="Tumor center"
)

plt.xlabel("x")
plt.ylabel("y")
plt.title("CAR-T Spatial Distribution at Tick 200")
plt.legend()
plt.axis("equal")
plt.tight_layout()
plt.savefig("cart_infiltration.png", dpi=300)
plt.close()


print("\n=== CAR-T RADIAL DISTANCE ===")

for pop in ["CAR-T CD4", "CAR-T CD8"]:
    subset = cart[cart["population"] == pop]

    print(
        f"{pop}: "
        f"mean = {subset['radius'].mean():.2f}, "
        f"min = {subset['radius'].min():.2f}, "
        f"max = {subset['radius'].max():.2f}"
    )

print("\n=== ECM DENSITY BY RADIAL DISTANCE ===")

ecm_rows = []

for entry in layers:
    coord = entry["location"]
    x, y, z = coord[:3]

    ecm_rows.append({
        "x": x,
        "y": y,
        "z": z,
        "ecm": entry["layers"]["ECM_DENSITY"],
    })

ecm_df = pd.DataFrame(ecm_rows)

ecm_df["radius"] = np.sqrt(
    (ecm_df["x"] - center_x) ** 2
    + (ecm_df["y"] - center_y) ** 2
    + (ecm_df["z"] - center_z) ** 2
)

ecm_bins = [0, 5, 10, 15, 20, 30]
ecm_labels = ["0-5", "5-10", "10-15", "15-20", "20-30"]

ecm_df["radial_bin"] = pd.cut(
    ecm_df["radius"],
    bins=ecm_bins,
    labels=ecm_labels,
    include_lowest=True
)

print(
    ecm_df.groupby("radial_bin", observed=False)["ecm"]
    .agg(["mean", "min", "max"])
    .round(3)
)

print("\nAnalysis complete.")
print("Created:")
print("  cancer_states.png")
print("  cancer_radial_states.png")
print("  cart_infiltration.png")

# --- Cell spatial map ---

plt.figure(figsize=(9, 9))

colors = {
    "Cancer": "crimson",
    "CAR-T CD4": "royalblue",
    "CAR-T CD8": "seagreen",
}

markers = {
    "Cancer": "o",
    "CAR-T CD4": "^",
    "CAR-T CD8": "s",
}

for population in ["Cancer", "CAR-T CD4", "CAR-T CD8"]:
    subset = df[df["population"] == population]

    plt.scatter(
        subset["x"],
        subset["y"],
        c=colors[population],
        marker=markers[population],
        s=90,
        edgecolors="black",
        linewidths=0.5,
        label=population,
    )

plt.scatter(
    center_x,
    center_y,
    marker="x",
    c="black",
    s=180,
    linewidths=3,
    label="Tumor center",
)

plt.xlabel("x")
plt.ylabel("y")
plt.title("ARCADE Cell Distribution at Tick 200")
plt.legend()
plt.axis("equal")
plt.tight_layout()

plt.savefig(
    "output/output_four/tick200_cell_map.png",
    dpi=300,
)

plt.close()


# --- ECM density map ---

plt.figure(figsize=(9, 9))

scatter = plt.scatter(
    ecm_df["x"],
    ecm_df["y"],
    c=ecm_df["ecm"],
    cmap="viridis",
    s=110,
    marker="s",
)

plt.scatter(
    center_x,
    center_y,
    marker="x",
    c="red",
    s=180,
    linewidths=3,
    label="Tumor center",
)

plt.colorbar(scatter, label="ECM density")
plt.xlabel("x")
plt.ylabel("y")
plt.title("ECM Density at Tick 200")
plt.legend()
plt.axis("equal")
plt.tight_layout()

plt.savefig(
    "output/output_four/tick200_ecm_map.png",
    dpi=300,
)

plt.close()


print("\nCreated visualizations:")
print("  output/output_four/tick200_cell_map.png")
print("  output/output_four/tick200_ecm_map.png")