package com.anywherecommerce.android.sdk.sampleapp.external;

import com.anywherecommerce.android.sdk.models.GatewayResponse;
import com.anywherecommerce.android.sdk.util.Amount;

public class TestGatewayResponse implements GatewayResponse {

    public TestGatewayResponse(String res) {

    }

    @Override
    public boolean isApproved() {
        return true;
    }

    @Override
    public boolean isPartiallyApproved() {
        return false;
    }

    @Override
    public String getID() {
        return "123456";
    }

    @Override
    public String getStatus() {
        return "TEST STATUS";
    }

    @Override
    public Amount getApprovedAmount() {
        return new Amount("1");
    }

    @Override
    public String getApprovalCode() {
        return "APPROVAL CODE";
    }

    @Override
    public String getResponseText() {
        return "TEST RESPONSE";
    }

    @Override
    public String getEmvPayload() {
        return "8A023030";
    }
}
