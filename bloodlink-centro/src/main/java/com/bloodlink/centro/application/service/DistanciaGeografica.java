package com.bloodlink.centro.application.service;

public final class DistanciaGeografica {

    private static final double RAIO_TERRA_KM = 6371.0088;

    private DistanciaGeografica() {
    }

    /** Distância em km pela fórmula de Haversine. */
    public static double km(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return RAIO_TERRA_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}