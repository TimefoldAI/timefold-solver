package ai.timefold.solver.service.maps.service.integration.internal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MapServiceOptions {

    public static final String PROVIDER = "provider";

    public static final String LOCATION = "location";

    public static final String MODEL = "model";

    public static final String MODEL_VERSION = "modelVersion";

    public static final String MODEL_RESOURCE = "modelResource";

    public static final String TENANT_ID = "tenantId";

    public static final String LOCATION_SET_NAME = "locationSetName";

    public static final String MAX_DISTANCE_FROM_ROAD = "maxDistanceFromRoad";

    public static final String TRANSPORT_TYPE = "transportType";

    public static Map<String, String> parse(String options) {
        Map<String, String> optionMap = new HashMap<>();

        if (options == null || options.isEmpty()) {
            return optionMap;
        }

        String[] optionList = options.split(",");
        for (String option : optionList) {
            String[] keyValue = option.split(":");

            if (keyValue.length != 2) {
                throw new IllegalArgumentException("Illegal options provided: '" + Arrays.toString(keyValue) + "'.");
            }

            optionMap.put(keyValue[0].trim(), keyValue[1].trim());
        }
        return optionMap;
    }

    public static String getProviderOption(String provider) {
        if (provider == null) {
            return "";
        }
        return PROVIDER + ":" + provider;
    }

    public static String getLocationOption(String location) {
        if (location == null) {
            return "";
        }
        return LOCATION + ":" + location;
    }

    public static String getModelOption(String model) {
        if (model == null) {
            return "";
        }
        return MODEL + ":" + model;
    }

    public static String getModelVersionOption(String modelVersion) {
        if (modelVersion == null) {
            return "";
        }
        return MODEL_VERSION + ":" + modelVersion;
    }

    public static String getModelResourceOption(String modelResource) {
        if (modelResource == null) {
            return "";
        }
        return MODEL_RESOURCE + ":" + modelResource;
    }

    public static String getTenantIdOption(String tenantId) {
        if (tenantId == null) {
            return "";
        }
        return TENANT_ID + ":" + tenantId;
    }

    public static String getLocationSetNameOption(String locationSetName) {
        if (locationSetName == null) {
            return "";
        }
        return LOCATION_SET_NAME + ":" + locationSetName;
    }

    public static String getMaxDistanceFromRoadOption(Double maxDistanceFromRoad) {
        if (maxDistanceFromRoad == null) {
            return "";
        }
        return MAX_DISTANCE_FROM_ROAD + ":" + maxDistanceFromRoad;
    }

    public static String getTransportTypeOption(String transportType) {
        if (transportType == null) {
            return "";
        }
        return TRANSPORT_TYPE + ":" + transportType;
    }

}
