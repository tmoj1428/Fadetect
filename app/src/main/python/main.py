from db.utils import get_connection
from data.read import read_data
import numpy as np
from scipy import stats
from utils.distance import haversine
import pandas as pd
import json

def prepare_data(database_path: str) -> pd.DataFrame:
    conn = get_connection(database_path)
    df = read_data(conn)
    return df

def calc_distances(df: pd.DataFrame):
    base_station_loc = df.iloc[0][['latitude', 'longitude']]
    df['distance'] = df.apply(lambda row: haversine(base_station_loc['latitude'], base_station_loc['longitude'], row['latitude'], row['longitude']), axis=1)

def main(database_directory: str):
    df = prepare_data(database_directory)
    calc_distances(df)

    df = df[df['distance'] > 0.0]
    
    # Pr = P0 - 10 * beta * log10(d/d0) + σ²

    Pr = df['rsrp'].to_numpy()
    d = df['distance'].to_numpy()

    d0 = 1.0  # Reference distance (d0) in km, to match the unit of our calculated distances
    X = 10 * np.log10(d / d0)

    # Perform linear regression
    slope, intercept, _, _, _ = stats.linregress(X, Pr)

    beta_estimate = -slope
    P0_estimate = intercept

    # Estimate the noise variance (sigma squared) by calculating the residuals
    residuals = Pr - (P0_estimate - beta_estimate * X)
    sigma_squared_estimate = np.var(residuals)

    estimated_params = {
        "Transmitted Power (P0)": P0_estimate,
        "Path Loss Exponent (beta)": beta_estimate,
        "Gaussian Noise Variance": sigma_squared_estimate
    }

    json_output = json.dumps(estimated_params)

    print("Estimated Parameters:\n")
    print("Transmitted Power (P0) : {:.2f} dBm".format(P0_estimate))
    print("Path Loss Exponent (beta)  : {:.2f}".format(beta_estimate))
    print("Gaussian Noise Variance : {:.2f}".format(sigma_squared_estimate))

    return '\n'.join([f'{key}: {value}' for key, value in estimated_params.items()])
