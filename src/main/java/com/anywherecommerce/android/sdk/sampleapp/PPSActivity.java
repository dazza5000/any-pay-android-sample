package com.anywherecommerce.android.sdk.sampleapp;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.anywherecommerce.android.sdk.AppBackgroundingManager;
import com.anywherecommerce.android.sdk.AuthenticationListener;
import com.anywherecommerce.android.sdk.Endpoint;
import com.anywherecommerce.android.sdk.GenericEventListener;
import com.anywherecommerce.android.sdk.GenericEventListenerWithParam;
import com.anywherecommerce.android.sdk.Logger;
import com.anywherecommerce.android.sdk.MeaningfulError;
import com.anywherecommerce.android.sdk.MeaningfulErrorListener;
import com.anywherecommerce.android.sdk.MeaningfulMessage;
import com.anywherecommerce.android.sdk.RequestListener;
import com.anywherecommerce.android.sdk.SDKManager;
import com.anywherecommerce.android.sdk.Terminal;
import com.anywherecommerce.android.sdk.component.SignatureView;
import com.anywherecommerce.android.sdk.devices.CardReader;
import com.anywherecommerce.android.sdk.devices.CardReaderController;
import com.anywherecommerce.android.sdk.devices.MultipleBluetoothDevicesFoundListener;
import com.anywherecommerce.android.sdk.devices.bbpos.BBPOSDevice;
import com.anywherecommerce.android.sdk.devices.bbpos.BBPOSDeviceCardReaderController;
import com.anywherecommerce.android.sdk.endpoints.AnyPayTransaction;
import com.anywherecommerce.android.sdk.endpoints.prioritypayments.PriorityPaymentsEndpoint;
import com.anywherecommerce.android.sdk.endpoints.prioritypayments.PriorityPaymentsGatewayResponse;
import com.anywherecommerce.android.sdk.endpoints.propay.PropayEndpoint;
import com.anywherecommerce.android.sdk.models.TransactionType;
import com.anywherecommerce.android.sdk.transactions.CardTransaction;
import com.anywherecommerce.android.sdk.transactions.Transaction;
import com.anywherecommerce.android.sdk.transactions.listener.CardTransactionListener;
import com.anywherecommerce.android.sdk.transactions.listener.TransactionListener;
import com.anywherecommerce.android.sdk.util.Amount;

import java.util.List;

public class PPSActivity extends Activity {

    TextView txtPanel;
    EditText txtReferenceId;
    Button btUSBConnect, btnConnectAudio, btnDisconnectAudio, btnIsDeviceConnected, btnStartEMV, btnConnectBT, btnDisconnectBT, btnGetTransactions, btnTerminalLogin, btnKeyedsale;
    Button btnCaptureTransaction, btnRefRefund;
    DialogManager dialogs = new DialogManager();
    Transaction refTransaction;
    Endpoint endpoint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pps);

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

                dialogs.showProgressDialog(PPSActivity.this, "Please Wait...");

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
                dialogs.showProgressDialog(PPSActivity.this, "Please Wait...");

                sendKeyedTransaction();
            }

            ;
        });


        btnCaptureTransaction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                addText("----> Starting New Refund Transaction <----");
                dialogs.showProgressDialog(PPSActivity.this, "Please Wait...");

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
                            PPSActivity.this.addText("----> Transaction Refunded <----");
                        else {
                            PPSActivity.this.addText(transaction.getResponseText());
                        }
                    }

                    @Override
                    public void onTransactionFailed(MeaningfulError meaningfulError) {
                        dialogs.hideProgressDialog();

                        PPSActivity.this.addText("Refund Failed");
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
                transaction.setTotalAmount(refTransaction.getTotalAmount());
                transaction.setRefTransactionId(refTransaction.getExternalId());

                transaction.setTransactionType(TransactionType.VOID);

                //t.enableLogging();
                transaction.execute(new TransactionListener() {

                    @Override
                    public void onTransactionCompleted() {
                        dialogs.hideProgressDialog();

                        if (transaction.isApproved())
                            PPSActivity.this.addText("----> Transaction Refunded <----");
                        else {
                            PPSActivity.this.addText(transaction.getResponseText());
                        }

                    }

                    @Override
                    public void onTransactionFailed(MeaningfulError reason) {
                        dialogs.hideProgressDialog();

                        PPSActivity.this.addText("Refund Failed");
                    }
                });
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

            }

            @Override
            public void onTransactionFailed(MeaningfulError reason) {
                dialogs.hideProgressDialog();
                addText("\r\n------>onTransactionFailed: " + reason.toString());

            }
        });
    }

    static boolean latch = false;

    private void addText(String text) {
        txtPanel.append("\r\n" + text + "\r\n\n");
    }

    private void authenticateTerminal() {

        addText("Authenticating. Please Wait...");

        ((PriorityPaymentsEndpoint)endpoint).setConsumerKey("");
        ((PriorityPaymentsEndpoint)endpoint).setSecret("");
        ((PriorityPaymentsEndpoint)endpoint).setMerchantId("");

        ((PriorityPaymentsEndpoint)endpoint).setUrl("https://sandbox.api.mxmerchant.com/checkout/v3/");

        ((PriorityPaymentsEndpoint)endpoint).authenticate(new AuthenticationListener() {
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
            addText("\r\n------>onTransactionCompleted - " + ((PriorityPaymentsGatewayResponse)refTransaction.getGatewayResponse()).getResponseJson());
        }

        @Override
        public void onTransactionFailed(MeaningfulError reason) {
            dialogs.hideProgressDialog();
            addText("\r\n------>onTransactionFailed: " + reason.toString());
        }
    };
}
