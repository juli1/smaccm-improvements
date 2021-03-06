package com.rockwellcollins.atc.resolute.validation;

import org.osate.aadl2.Access;
import org.osate.aadl2.ComponentCategory;
import org.osate.aadl2.instance.FeatureInstance;

public class BaseType extends ResoluteType {
    public static final BaseType INT = new BaseType("int");
    public static final BaseType REAL = new BaseType("real");
    public static final BaseType STRING = new BaseType("string");
    public static final BaseType BOOL = new BaseType("bool");
    public static final BaseType FAIL = new BaseType("fail");
    public static final BaseType RANGE = new BaseType("range");

    public static final BaseType AADL = new BaseType("aadl");

    public static final BaseType COMPONENT = new BaseType("component");
    public static final BaseType ABSTRACT = new BaseType("abstract");
    public static final BaseType BUS = new BaseType("bus");
    public static final BaseType DATA = new BaseType("data");
    public static final BaseType DEVICE = new BaseType("device");
    public static final BaseType MEMORY = new BaseType("memory");
    public static final BaseType PROCESSOR = new BaseType("processor");
    public static final BaseType PROCESS = new BaseType("process");
    public static final BaseType SUBPROGRAM_GROUP = new BaseType("subprogram_group");
    public static final BaseType SUBPROGRAM = new BaseType("subprogram");
    public static final BaseType SYSTEM = new BaseType("system");
    public static final BaseType THREAD_GROUP = new BaseType("thread_group");
    public static final BaseType THREAD = new BaseType("thread");
    public static final BaseType VIRTUAL_BUS = new BaseType("virtual_bus");
    public static final BaseType VIRTUAL_PROCESSOR = new BaseType("virtual_processor");

    public static final BaseType CONNECTION = new BaseType("connection");

    public static final BaseType PROPERTY = new BaseType("property");

    public static final BaseType FEATURE = new BaseType("feature");
    public static final BaseType PORT = new BaseType("port");
    public static final BaseType DATA_PORT = new BaseType("data_port");
    public static final BaseType EVENT_PORT = new BaseType("event_port");
    public static final BaseType EVENT_DATA_PORT = new BaseType("event_data_port");
    public static final BaseType FEATURE_GROUP = new BaseType("feature_group");
    public static final BaseType ACCESS = new BaseType("access");
    public static final BaseType BUS_ACCESS = new BaseType("bus_access");
    public static final BaseType PROVIDES_BUS_ACCESS = new BaseType("provides_bus_access");
    public static final BaseType REQUIRES_BUS_ACCESS = new BaseType("requires_bus_access");
    public static final BaseType DATA_ACCESS = new BaseType("data_access");
    public static final BaseType PROVIDES_DATA_ACCESS = new BaseType("provides_data_access");
    public static final BaseType REQUIRES_DATA_ACCESS = new BaseType("requires_data_access");
    public static final BaseType SUBPROGRAM_ACCESS = new BaseType("subprogram_access");
    public static final BaseType PROVIDES_SUBPROGRAM_ACCESS = new BaseType(
            "provides_subprogram_access");
    public static final BaseType REQUIRES_SUBPROGRAM_ACCESS = new BaseType(
            "requires_subprogram_access");
    public static final BaseType SUBPROGRAM_GROUP_ACCESS = new BaseType("subprogram_group_access");
    public static final BaseType PROVIDES_SUBPROGRAM_GROUP_ACCESS = new BaseType(
            "provides_subprogram_group_access");
    public static final BaseType REQUIRES_SUBPROGRAM_GROUP_ACCESS = new BaseType(
            "requires_subprogram_group_access");

    final public String name;

    public BaseType(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException();
        }

        this.name = name;
    }

    public BaseType(ComponentCategory category) {
        this.name = category.getName().replace(" ", "_");
    }

    public BaseType(FeatureInstance fi) {
        String prefix = "";
        if (fi.getFeature() instanceof Access) {
            Access acc = (Access) fi.getFeature();
            prefix = acc.getKind().toString() + "_";
        }
        this.name = prefix + convertCamelCase(fi.getCategory().toString());
    }

    private String convertCamelCase(String str) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (Character.isUpperCase(c)) {
                result.append('_');
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean subtypeOf(ResoluteType otherType) {
        if (this.equals(FAIL) || otherType.equals(FAIL)) {
            return true;
        }

        if (this.equals(otherType)) {
            return true;
        }

        if (otherType instanceof BaseType) {
            BaseType bt = (BaseType) otherType;
            switch (bt.name) {
            case "aadl":
                return subtypeOf(COMPONENT) || subtypeOf(CONNECTION) || subtypeOf(PROPERTY)
                        || subtypeOf(FEATURE);

            case "component":
                return subtypeOf(ABSTRACT) || subtypeOf(BUS) || subtypeOf(DATA)
                        || subtypeOf(DEVICE) || subtypeOf(MEMORY) || subtypeOf(PROCESSOR)
                        || subtypeOf(PROCESS) || subtypeOf(SUBPROGRAM_GROUP)
                        || subtypeOf(SUBPROGRAM) || subtypeOf(SYSTEM) || subtypeOf(THREAD_GROUP)
                        || subtypeOf(THREAD) || subtypeOf(VIRTUAL_BUS)
                        || subtypeOf(VIRTUAL_PROCESSOR);

            case "feature":
                return subtypeOf(PORT) || subtypeOf(ACCESS) || subtypeOf(FEATURE_GROUP);

            case "port":
                return subtypeOf(DATA_PORT) || subtypeOf(EVENT_PORT) || subtypeOf(EVENT_DATA_PORT);

            case "access":
                return subtypeOf(BUS_ACCESS) || subtypeOf(DATA_ACCESS)
                        || subtypeOf(SUBPROGRAM_ACCESS) || subtypeOf(SUBPROGRAM_GROUP_ACCESS);

            case "bus_access":
                return subtypeOf(PROVIDES_BUS_ACCESS) || subtypeOf(REQUIRES_BUS_ACCESS);

            case "data_access":
                return subtypeOf(PROVIDES_DATA_ACCESS) || subtypeOf(REQUIRES_DATA_ACCESS);

            case "subprogram_access":
                return subtypeOf(PROVIDES_SUBPROGRAM_ACCESS)
                        || subtypeOf(REQUIRES_SUBPROGRAM_ACCESS);

            case "subprogram_group_access":
                return subtypeOf(PROVIDES_SUBPROGRAM_GROUP_ACCESS)
                        || subtypeOf(REQUIRES_SUBPROGRAM_GROUP_ACCESS);

            default:
                return false;
            }
        }

        return false;
    }

    @Override
    public ResoluteType join(ResoluteType otherType) {
        if (this.equals(FAIL) || otherType.equals(FAIL)) {
            return FAIL;
        }

        if (this.equals(otherType)) {
            return this;
        }

        if (subtypeOf(COMPONENT) && otherType.subtypeOf(COMPONENT)) {
            return COMPONENT;
        }

        if (subtypeOf(AADL) && otherType.subtypeOf(AADL)) {
            return AADL;
        }

        return null;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BaseType) {
            BaseType bt = (BaseType) obj;
            return bt.name.equals(name);
        }

        return false;
    }
}
