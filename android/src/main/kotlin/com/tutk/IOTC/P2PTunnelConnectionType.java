/**
 * P2PTunnelConnectionType.java
 *
 * Copyright (c) by TUTK Co.LTD. All Rights Reserved.
 */
package com.tutk.IOTC;

/**
* Enum the tunnel connection type
*/
public enum P2PTunnelConnectionType {
    TUNNEL_CONNECT_AUTO(0),
    TUNNEL_CONNECT_MANUAL(1),
    TUNNEL_CONNECT_COUNT(2);

    private int value;
    private P2PTunnelConnectionType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}