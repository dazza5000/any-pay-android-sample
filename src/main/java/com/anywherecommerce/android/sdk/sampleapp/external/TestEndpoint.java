package com.anywherecommerce.android.sdk.sampleapp.external;

import android.text.TextUtils;

import com.anywherecommerce.android.sdk.CommonErrors;
import com.anywherecommerce.android.sdk.Endpoint;
import com.anywherecommerce.android.sdk.Logger;

import com.anywherecommerce.android.sdk.RequestListener;

import com.anywherecommerce.android.sdk.endpoints.AnyPayTransaction;

import com.anywherecommerce.android.sdk.models.GatewayResponse;
import com.anywherecommerce.android.sdk.models.TransactionStatus;
import com.anywherecommerce.android.sdk.transactions.Transaction;


import java.util.Date;


public class TestEndpoint implements Endpoint {


    @Override
    public void submitTransaction(Transaction t, RequestListener<GatewayResponse> requestListener) {
        AnyPayTransaction transaction = (AnyPayTransaction)t;
        switch (transaction.getTransactionType())
        {
            case AUTHONLY:
                submitAuthRequest(transaction, requestListener);
                break;
            case SALE:
                submitAuthRequest(transaction, requestListener);
                break;
            case CAPTURE:
                submitAuthRequest(transaction, requestListener);
                break;
            case REFUND:
            case VOID:
            case REVERSEAUTH:
                submitAuthRequest(transaction, requestListener);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported transaction type " + t.getTransactionType().toString());
        }
    }

    @Override
    public void fetchTransactions(int i, int i1, String s, Date date, Date date1, RequestListener requestListener) {

    }

    @Override
    public void updateTransaction(Transaction transaction, RequestListener<GatewayResponse> requestListener) {

    }

    @Override
    public String getUrl() {
        return "";
    }

    @Override
    public void setUrl(String s) {

    }

    @Override
    public String getFlavor() {
        return null;
    }

    @Override
    public String getProvider() {
        return null;
    }


    public void submitAuthRequest(final AnyPayTransaction t, final RequestListener<GatewayResponse> requestListener) {
        if ( ((AnyPayTransaction) t).getEncryptedSwipe() != null )
            submitSwipeAuthRequest(t, requestListener);
        else if ( ((AnyPayTransaction) t).getCVV2() != null )
            submitKeyedAuthRequest(t, requestListener);
        else
            submitEmvAuthRequest(t, requestListener);
    }


    public void submitSwipeAuthRequest(final AnyPayTransaction t, final RequestListener<GatewayResponse> requestListener) {
        try {
            //Write code for sending request to gateway api


            requestListener.onRequestComplete(new TestGatewayResponse(""));
        } catch (Exception ex) {
            Logger.logException(ex);
        }

    }

    public void submitKeyedAuthRequest(final AnyPayTransaction t, final RequestListener<GatewayResponse> requestListener) {
        try {

            //Write code for sending request to gateway api


            requestListener.onRequestComplete(new TestGatewayResponse(""));

        } catch (Exception ex) {
            Logger.logException(ex);
        }
    }

    public void submitEmvAuthRequest(final AnyPayTransaction t, final RequestListener<GatewayResponse> requestListener) {

        try {
            //Write code for sending request to gateway api


            requestListener.onRequestComplete(new TestGatewayResponse(""));


        } catch (Exception ex) {
            Logger.logException(ex);
        }
    }

}
