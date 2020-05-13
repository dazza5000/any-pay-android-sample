package com.anywherecommerce.android.sdk.sampleapp;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.anywherecommerce.android.sdk.AppBackgroundingManager;
import com.anywherecommerce.android.sdk.AuthenticationListener;
import com.anywherecommerce.android.sdk.Endpoint;
import com.anywherecommerce.android.sdk.Logger;
import com.anywherecommerce.android.sdk.MeaningfulError;
import com.anywherecommerce.android.sdk.MeaningfulErrorListener;
import com.anywherecommerce.android.sdk.MeaningfulMessage;
import com.anywherecommerce.android.sdk.SDKManager;
import com.anywherecommerce.android.sdk.Terminal;
import com.anywherecommerce.android.sdk.component.SignatureView;
import com.anywherecommerce.android.sdk.devices.BluetoothCardReaderConnectionListener;
import com.anywherecommerce.android.sdk.devices.CardReaderConnectionListener;
import com.anywherecommerce.android.sdk.devices.CardReaderController;
import com.anywherecommerce.android.sdk.devices.TransactionWorkflow;
import com.anywherecommerce.android.sdk.devices.USBCardReaderConnectionListener;
import com.anywherecommerce.android.sdk.devices.bbpos.BBPOSDevice;
import com.anywherecommerce.android.sdk.devices.bbpos.BBPOSDeviceCardReaderController;
import com.anywherecommerce.android.sdk.devices.CardReader;
import com.anywherecommerce.android.sdk.devices.MultipleBluetoothDevicesFoundListener;
import com.anywherecommerce.android.sdk.RequestListener;
import com.anywherecommerce.android.sdk.GenericEventListener;
import com.anywherecommerce.android.sdk.GenericEventListenerWithParam;
import com.anywherecommerce.android.sdk.devices.bbpos.WalkerC2X;
import com.anywherecommerce.android.sdk.endpoints.AnyPayTransaction;
import com.anywherecommerce.android.sdk.endpoints.prioritypayments.PriorityPaymentsEndpoint;
import com.anywherecommerce.android.sdk.endpoints.propay.PropayEndpoint;

import com.anywherecommerce.android.sdk.endpoints.worldnet.WorldnetEndpoint;
import com.anywherecommerce.android.sdk.models.CustomerDetails;
import com.anywherecommerce.android.sdk.models.TipLineItem;
import com.anywherecommerce.android.sdk.models.TransactionType;

import com.anywherecommerce.android.sdk.transactions.CardTransaction;
import com.anywherecommerce.android.sdk.transactions.Transaction;
import com.anywherecommerce.android.sdk.transactions.listener.CardTransactionListener;
import com.anywherecommerce.android.sdk.transactions.listener.TransactionListener;
import com.anywherecommerce.android.sdk.util.Amount;

import java.util.List;


/**
 * Created by Admin on 10/4/2017.
 */

public class MainActivity extends Activity {

    TextView txtPanel;
    EditText txtReferenceId;
    Button btUSBConnect, btnConnectAudio, btnDisconnectAudio, btnIsDeviceConnected, btnStartEMV, btnConnectBT, btnDisconnectBT, btnGetTransactions, btnTerminalLogin, btnKeyedsale;
    Button btnCaptureTransaction, btnRefRefund;
    DialogManager dialogs = new DialogManager();
    Transaction refTransaction;
    Endpoint endpoint;
    String propaySessionID;
    Button btnSubmitSignature;
    LinearLayout signatureViewLayout;
    SignatureView signatureView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.testharness_device);

        if (!PermissionsController.verifyAppPermissions(this)) {
            PermissionsController.requestAppPermissions(this, PermissionsController.permissions, 1001);
        }

        Logger.i("SDK Version - " + SDKManager.getSdkVersion());

        Terminal.initialize();
        endpoint = Terminal.getInstance().getEndpoint();

        AppBackgroundingManager.get().registerListener(new AppBackgroundingManager.AppBackroundedListener() {
            @Override
            public void onBecameForeground() {
                Logger.trace("Caught app in foreground.");
            }

            @Override
            public void onBecameBackground() {
                Logger.trace("Caught app in background.");
            }
        });

        txtPanel = (TextView) findViewById(R.id.txtTextHarnessPanel);
        txtPanel.setMovementMethod(new ScrollingMovementMethod());

        btnConnectAudio = (Button) findViewById(R.id.audioConnect);
        btnDisconnectAudio = (Button) findViewById(R.id.btnDisconnectAudio);
        btnConnectBT = (Button) findViewById(R.id.btConnect);
        btnDisconnectBT = (Button) findViewById(R.id.btnDisconnectBT);
        btnIsDeviceConnected = (Button) findViewById(R.id.btnIsDeviceConnected);
        btnGetTransactions = (Button) findViewById(R.id.btnGetTransactions);
        btnStartEMV = (Button) findViewById(R.id.btnStartEmvSale);
        btnTerminalLogin = (Button) findViewById(R.id.terminalLogin);
        btnKeyedsale = (Button) findViewById(R.id.keyedsale);
        btnCaptureTransaction = (Button) findViewById(R.id.unrefRefund);
        btnRefRefund = (Button) findViewById(R.id.refRefund);
        txtReferenceId = (EditText) findViewById(R.id.txtReferenceId);
        btUSBConnect = (Button) findViewById(R.id.btUSBConnect);
        signatureViewLayout = (LinearLayout) findViewById(R.id.signatureViewLayout);
        signatureView = (SignatureView) findViewById(R.id.signatureView);
        btnSubmitSignature = (Button) findViewById(R.id.btnSubmitSignature);

        signatureView.setStrokeColor(Color.MAGENTA);
        signatureView.setStrokeWidth(10f);


        final CardReaderController cardReaderController = CardReaderController.getControllerFor(BBPOSDevice.class);

        cardReaderController.subscribeOnCardReaderConnected(new GenericEventListenerWithParam<CardReader>() {
            @Override
            public void onEvent(CardReader deviceInfo) {
                if (deviceInfo == null)
                    addText("\r\nUnknown device connected");
                else
                    addText("\nDevice connected " + deviceInfo.getModelDisplayName());
            }
        });

        cardReaderController.subscribeOnCardReaderDisconnected(new GenericEventListener() {
            @Override
            public void onEvent() {
                addText("\nDevice disconnected");
            }
        });

        cardReaderController.subscribeOnCardReaderConnectFailed(new MeaningfulErrorListener() {
            @Override
            public void onError(MeaningfulError error) {
                addText("\nDevice connect failed: " + error.toString());
            }
        });

        cardReaderController.subscribeOnCardReaderError(new MeaningfulErrorListener() {
            @Override
            public void onError(MeaningfulError error) {
                addText("\nDevice error: " + error.toString());
            }
        });

        btUSBConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addText("\nConnecting to USB Reader\r\n");
                cardReaderController.connectOther(CardReader.ConnectionMethod.USB);
            }
        });

        btnConnectAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addText("\nConnecting to audio jack (with polling)\r\n");
                cardReaderController.connectAudioJack();
            }
        });

        btnDisconnectAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addText("\r\nNot implemented");
                //addText("Disconnecting audio jack\r\n");
                //BBPOSDeviceCardReaderController.getInstance().disconnectAudioJack();
            }
        });

        btnConnectBT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addText("\nConnecting to BT\r\n");
                cardReaderController.connectBluetooth(new MultipleBluetoothDevicesFoundListener() {
                    @Override
                    public void onMultipleBluetoothDevicesFound(List<BluetoothDevice> matchingDevices) {
                        addText("Many BT devices");
                    }
                });
            }
        });

        btnDisconnectBT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addText("Disconnecting bluetooth\r\n");
                cardReaderController.disconnectReader();
            }
        });


        btnIsDeviceConnected.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addText("\nClicky");
                BBPOSDeviceCardReaderController.getController().setDebugLogEnabled(true);
                if (latch) {
                    addText("\r\nStopping audio");
                    cardReaderController.disconnectReader();
                } else {
                    addText("\r\nStarting audio");
                    cardReaderController.connectAudioJack();
                }

                latch = !latch;
            }
        });

        btnStartEMV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BBPOSDeviceCardReaderController.getController().setDebugLogEnabled(true);

                addText("\nStarting EMV transaction");
                if (!CardReaderController.isCardReaderConnected()) {
                    addText("\r\nNo card reader connected");
                    return;
                }

                dialogs.showProgressDialog(MainActivity.this, "Please Wait...");

                sendEMVTransaction();
            }
        });



        btnTerminalLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                authenticateTerminal();
            }
        });

        btnKeyedsale.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addText("\r\nExecuting Keyed Sale Transaction. Please Wait...");
                dialogs.showProgressDialog(MainActivity.this, "Please Wait...");

                sendKeyedTransaction();
            }

            ;
        });


        btnCaptureTransaction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                addText("----> Starting New Refund Transaction <----");
                dialogs.showProgressDialog(MainActivity.this, "Please Wait...");

                final AnyPayTransaction transaction = new AnyPayTransaction();
                transaction.setEndpoint(endpoint);
                transaction.setTransactionType(TransactionType.REFUND);
                transaction.setTotalAmount(new Amount("20.25"));
                transaction.setCardExpiryMonth("10");
                transaction.setCardExpiryYear("20");
                transaction.setAddress("123 Main Street");
                transaction.setPostalCode("30004");
                transaction.setCVV2("999");
                transaction.setCardholderName("Jane Doe");
                transaction.setCardNumber("4012888888881881");
                transaction.execute(new TransactionListener() {
                    @Override
                    public void onTransactionCompleted() {
                        dialogs.hideProgressDialog();

                        if (transaction.isApproved())
                            MainActivity.this.addText("----> Transaction Refunded <----");
                        else {
                            MainActivity.this.addText(transaction.getResponseText());
                        }
                    }

                    @Override
                    public void onTransactionFailed(MeaningfulError meaningfulError) {
                        dialogs.hideProgressDialog();

                        MainActivity.this.addText("Refund Failed");
                    }
                });
            }
        });

        btnRefRefund.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (refTransaction == null) {
                    addText("----> This voids the last Auth transaction made. Please perform a Auth transaction first. <----");
                    return;
                }

                addText("----> Starting Referenced Refund Transaction <----");

                final AnyPayTransaction transaction = new AnyPayTransaction();
                transaction.setEndpoint(endpoint);
                transaction.setExternalId(refTransaction.getExternalId());
                transaction.setTotalAmount(new Amount("1"));
                transaction.setRefTransactionId(refTransaction.getExternalId());
                transaction.setTransactionType(TransactionType.REFUND);

                //t.enableLogging();
                transaction.execute(new TransactionListener() {

                    @Override
                    public void onTransactionCompleted() {
                        dialogs.hideProgressDialog();

                        if (transaction.isApproved())
                            MainActivity.this.addText("----> Transaction Refunded <----");
                        else {
                            MainActivity.this.addText(transaction.getResponseText());
                        }

                    }

                    @Override
                    public void onTransactionFailed(MeaningfulError reason) {
                        dialogs.hideProgressDialog();

                        MainActivity.this.addText("Refund Failed");
                    }
                });
            }
        });

        btnSubmitSignature.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogs.showProgressDialog(MainActivity.this, "Please Wait...");
                ((CardTransaction)refTransaction).setSignature(signatureView.getSignature());

                if (refTransaction.getCustomField("completed") != null) {

                    ((WorldnetEndpoint)endpoint).submitSignature(refTransaction, new RequestListener() {
                        @Override
                        public void onRequestComplete(Object o) {
                            addText("\r\n SIgnature Sent Successfully");
                        }

                        @Override
                        public void onRequestFailed(MeaningfulError meaningfulError) {
                            addText("\r\n SIgnature sent Failed");
                        }
                    });
                }
                else {
                    refTransaction.proceed();
                }

                addText("\r\nSending SIgnature");

            }
        });

        authenticateTerminal();
    }


    private void sendKeyedTransaction() {
        final AnyPayTransaction transaction = new AnyPayTransaction();
        transaction.setEndpoint(endpoint);
        transaction.setTransactionType(TransactionType.SALE);
        transaction.setCardExpiryMonth("10");
        transaction.setCardExpiryYear("20");
        transaction.setAddress("123 Main Street");
        transaction.setPostalCode("30004");
        transaction.setCVV2("999");
        transaction.setCardholderName("Jane Doe");
        transaction.setCardNumber("4012888888881881");
        transaction.setTotalAmount(new Amount("10.47"));
        transaction.setCurrency("USD");

        refTransaction = transaction;
        //t.enableLogging();
        transaction.execute(transactionListener);

    }

    private void sendEMVTransaction() {
        final AnyPayTransaction transaction = new AnyPayTransaction();
        transaction.setEndpoint(endpoint);
        transaction.useCardReader(CardReaderController.getConnectedReader());
        transaction.setTransactionType(TransactionType.SALE);
        transaction.setAddress("123 Main Street");
        transaction.setPostalCode("30004");
        transaction.setTotalAmount(new Amount("10.47"));
        transaction.setCurrency("USD");

        transaction.setOnSignatureRequiredListener(new GenericEventListener() {
            @Override
            public void onEvent() {
                addText("\r\n------>onSignatureRequired: sending null and proceeding");
                String signature = "base64-encoded image or point map";
                dialogs.hideProgressDialog();

                signatureViewLayout.setVisibility(View.VISIBLE);
            }
        });

        refTransaction = transaction;

        //t.enableLogging();
        transaction.execute(new CardTransactionListener() {
            @Override
            public void onCardReaderEvent(MeaningfulMessage event) {
                addText("\r\n------>onCardReaderEvent: " + event.message);
            }

            @Override
            public void onTransactionCompleted() {
                dialogs.hideProgressDialog();
                addText("\r\n------>onTransactionCompleted" + transaction.isApproved().toString());

                signatureViewLayout.setVisibility(View.GONE);
            }

            @Override
            public void onTransactionFailed(MeaningfulError reason) {
                dialogs.hideProgressDialog();
                addText("\r\n------>onTransactionFailed: " + reason.toString());

                signatureViewLayout.setVisibility(View.GONE);
            }
        });
    }

    static boolean latch = false;

    private void addText(String text) {
        txtPanel.append("\r\n" + text + "\r\n\n");
    }

    private void authenticateTerminal() {

        addText("Authenticating. Please Wait...");


        ((WorldnetEndpoint)endpoint).setWorldnetSecret("");
        ((WorldnetEndpoint)endpoint).setWorldnetTerminalID("");

        ((WorldnetEndpoint)endpoint).setUrl("https://testpayments.anywherecommerce.com/merchant");


        ((WorldnetEndpoint)endpoint).authenticate(new AuthenticationListener() {
            @Override
            public void onAuthenticationComplete() {
                addText("\r\n------> Terminal Authenticated");
            }

            @Override
            public void onAuthenticationFailed(MeaningfulError meaningfulError) {
                addText("\r\n------> Terminal Authentication Failed");
            }
        });

    }

    private TransactionListener transactionListener = new TransactionListener() {
        @Override
        public void onTransactionCompleted() {
            dialogs.hideProgressDialog();
            addText("\r\n------>onTransactionCompleted - " + refTransaction.isApproved().toString());

            if (refTransaction.getTransactionType() == TransactionType.SALE) {
                final TipLineItem tip = new TipLineItem();
                tip.amount = new Amount("23");

                refTransaction.setTip(tip);

                ((WorldnetEndpoint)endpoint).submitTipAdjustment(refTransaction, tip, new RequestListener() {
                    @Override
                    public void onRequestComplete(Object o) {
                        addText("\r\n------>Tip Adjustment success");
                    }

                    @Override
                    public void onRequestFailed(MeaningfulError meaningfulError) {
                        addText("\r\n------>Tip Adjustment failed");
                    }
                });

                /*((AnyPayTransaction)refTransaction).update(new TransactionListener() {
                    @Override
                    public void onTransactionCompleted() {
                        addText("\r\n------>update transaction complete - " + refTransaction.isApproved().toString());

                        CustomerDetails customerDetails = new CustomerDetails();
                        customerDetails.setEmailAddress("theiosdevguy@gmail.com");
                        refTransaction.setCustomerDetails(customerDetails);
                        ((AnyPayTransaction) refTransaction).update(new TransactionListener() {
                            @Override
                            public void onTransactionCompleted() {
                                addText("\r\n------>Receipt Sent - ");

                                refTransaction.addCustomField("completed", true);
                                signatureViewLayout.setVisibility(View.VISIBLE);

                            }

                            @Override
                            public void onTransactionFailed(MeaningfulError meaningfulError) {
                                addText("\r\n------>Receipt Sent Failed ");
                            }
                        });
                    }

                    @Override
                    public void onTransactionFailed(MeaningfulError meaningfulError) {
                        addText("\r\n------>update transaction failed - " + refTransaction.isApproved().toString());
                    }
                });*/
            }
        }

        @Override
        public void onTransactionFailed(MeaningfulError reason) {
            dialogs.hideProgressDialog();
            addText("\r\n------>onTransactionFailed: " + reason.toString());
        }
    };
}
