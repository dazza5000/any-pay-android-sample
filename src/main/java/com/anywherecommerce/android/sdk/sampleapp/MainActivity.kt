package com.anywherecommerce.android.sdk.sampleapp

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.anywherecommerce.android.sdk.*
import com.anywherecommerce.android.sdk.AppBackgroundingManager.AppBackroundedListener
import com.anywherecommerce.android.sdk.component.SignatureView
import com.anywherecommerce.android.sdk.devices.CardReader
import com.anywherecommerce.android.sdk.devices.CardReaderController
import com.anywherecommerce.android.sdk.devices.bbpos.BBPOSDevice
import com.anywherecommerce.android.sdk.endpoints.AnyPayTransaction
import com.anywherecommerce.android.sdk.endpoints.worldnet.WorldnetEndpoint
import com.anywherecommerce.android.sdk.models.TipLineItem
import com.anywherecommerce.android.sdk.models.TransactionType
import com.anywherecommerce.android.sdk.sampleapp.MainActivity
import com.anywherecommerce.android.sdk.transactions.CardTransaction
import com.anywherecommerce.android.sdk.transactions.Transaction
import com.anywherecommerce.android.sdk.transactions.listener.CardTransactionListener
import com.anywherecommerce.android.sdk.transactions.listener.TransactionListener
import com.anywherecommerce.android.sdk.util.Amount
import com.bbpos.bbdevice.BBDeviceController

/**
 * Created by Admin on 10/4/2017.
 */
class MainActivity : Activity() {
    var txtPanel: TextView? = null
    var txtReferenceId: EditText? = null
    var btUSBConnect: Button? = null
    var btnConnectAudio: Button? = null
    var btnDisconnectAudio: Button? = null
    var btnIsDeviceConnected: Button? = null
    var btnStartEMV: Button? = null
    var btnConnectBT: Button? = null
    var btnDisconnectBT: Button? = null
    var btnGetTransactions: Button? = null
    var btnTerminalLogin: Button? = null
    var btnKeyedsale: Button? = null
    var btnCaptureTransaction: Button? = null
    var btnRefRefund: Button? = null
    var dialogs = DialogManager()
    var refTransaction: Transaction? = null
    var endpoint: Endpoint? = null
    var propaySessionID: String? = null
    var btnSubmitSignature: Button? = null
    var signatureViewLayout: LinearLayout? = null
    var signatureView: SignatureView? = null
    override fun onCreate(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.testharness_device)
        if (!PermissionsController.verifyAppPermissions(this)) {
            PermissionsController.requestAppPermissions(this, PermissionsController.permissions, 1001)
        }
        Logger.i("SDK Version - " + SDKManager.getSdkVersion())
        Terminal.initialize()
        endpoint = Terminal.getInstance().endpoint
        AppBackgroundingManager.get().registerListener(object : AppBackroundedListener {
            override fun onBecameForeground() {
                Logger.trace("Caught app in foreground.")
            }

            override fun onBecameBackground() {
                Logger.trace("Caught app in background.")
            }
        })
        txtPanel = findViewById<View>(R.id.txtTextHarnessPanel) as TextView
        txtPanel!!.movementMethod = ScrollingMovementMethod()
        btnConnectAudio = findViewById<View>(R.id.audioConnect) as Button
        btnDisconnectAudio = findViewById<View>(R.id.btnDisconnectAudio) as Button
        btnConnectBT = findViewById<View>(R.id.btConnect) as Button
        btnDisconnectBT = findViewById<View>(R.id.btnDisconnectBT) as Button
        btnIsDeviceConnected = findViewById<View>(R.id.btnIsDeviceConnected) as Button
        btnGetTransactions = findViewById<View>(R.id.btnGetTransactions) as Button
        btnStartEMV = findViewById<View>(R.id.btnStartEmvSale) as Button
        btnTerminalLogin = findViewById<View>(R.id.terminalLogin) as Button
        btnKeyedsale = findViewById<View>(R.id.keyedsale) as Button
        btnCaptureTransaction = findViewById<View>(R.id.unrefRefund) as Button
        btnRefRefund = findViewById<View>(R.id.refRefund) as Button
        txtReferenceId = findViewById<View>(R.id.txtReferenceId) as EditText
        btUSBConnect = findViewById<View>(R.id.btUSBConnect) as Button
        signatureViewLayout = findViewById<View>(R.id.signatureViewLayout) as LinearLayout
        signatureView = findViewById<View>(R.id.signatureView) as SignatureView
        btnSubmitSignature = findViewById<View>(R.id.btnSubmitSignature) as Button
        signatureView!!.strokeColor = Color.MAGENTA
        signatureView!!.strokeWidth = 10f
        val cardReaderController = CardReaderController.getControllerFor(BBPOSDevice::class.java)
        cardReaderController.subscribeOnCardReaderConnected { deviceInfo ->
            if (deviceInfo == null) addText("\r\nUnknown device connected") else addText("""

    Device connected ${deviceInfo.modelDisplayName}
    """.trimIndent())
        }
        cardReaderController.subscribeOnCardReaderDisconnected { addText("\nDevice disconnected") }
        cardReaderController.subscribeOnCardReaderConnectFailed { error -> addText("\nDevice connect failed: $error") }
        cardReaderController.subscribeOnCardReaderError { error -> addText("\nDevice error: $error") }
        btUSBConnect!!.setOnClickListener {
            addText("\nConnecting to USB Reader\r\n")
            cardReaderController.connectOther(CardReader.ConnectionMethod.USB)
        }
        btnConnectAudio!!.setOnClickListener {
            addText("\nConnecting to audio jack (with polling)\r\n")
            cardReaderController.connectAudioJack()
        }
        btnDisconnectAudio!!.setOnClickListener {
            addText("\r\nNot implemented")
            //addText("Disconnecting audio jack\r\n");
            //BBPOSDeviceCardReaderController.getInstance().disconnectAudioJack();
        }
        btnConnectBT!!.setOnClickListener {
            addText("\nConnecting to BT\r\n")
            cardReaderController.connectBluetooth { addText("Many BT devices") }
        }
        btnDisconnectBT!!.setOnClickListener {
            addText("Disconnecting bluetooth\r\n")
            cardReaderController.disconnectReader()
        }
        btnIsDeviceConnected!!.setOnClickListener {
            addText("\nClicky")
            BBDeviceController.setDebugLogEnabled(true)
            if (latch) {
                addText("\r\nStopping audio")
                cardReaderController.disconnectReader()
            } else {
                addText("\r\nStarting audio")
                cardReaderController.connectAudioJack()
            }
            latch = !latch
        }
        btnStartEMV!!.setOnClickListener(View.OnClickListener {
            BBDeviceController.setDebugLogEnabled(true)
            addText("\nStarting EMV transaction")
            if (!CardReaderController.isCardReaderConnected()) {
                addText("\r\nNo card reader connected")
                return@OnClickListener
            }
            dialogs.showProgressDialog(this@MainActivity, "Please Wait...")
            sendEMVTransaction()
        })
        btnTerminalLogin!!.setOnClickListener { authenticateTerminal() }
        btnKeyedsale!!.setOnClickListener {
            addText("\r\nExecuting Keyed Sale Transaction. Please Wait...")
            dialogs.showProgressDialog(this@MainActivity, "Please Wait...")
            sendKeyedTransaction()
        }
        btnCaptureTransaction!!.setOnClickListener {
            addText("----> Starting New Refund Transaction <----")
            dialogs.showProgressDialog(this@MainActivity, "Please Wait...")
            val transaction = AnyPayTransaction()
            transaction.endpoint = endpoint
            transaction.transactionType = TransactionType.REFUND
            transaction.totalAmount = Amount("20.25")
            transaction.cardExpiryMonth = "10"
            transaction.cardExpiryYear = "20"
            transaction.address = "123 Main Street"
            transaction.postalCode = "30004"
            transaction.cvV2 = "999"
            transaction.cardholderName = "Jane Doe"
            transaction.cardNumber = "4012888888881881"
            transaction.execute(object : TransactionListener {
                override fun onTransactionCompleted() {
                    dialogs.hideProgressDialog()
                    if (transaction.isApproved) addText("----> Transaction Refunded <----") else {
                        addText(transaction.responseText)
                    }
                }

                override fun onTransactionFailed(meaningfulError: MeaningfulError) {
                    dialogs.hideProgressDialog()
                    addText("Refund Failed")
                }
            })
        }
        btnRefRefund!!.setOnClickListener(View.OnClickListener {
            if (refTransaction == null) {
                addText("----> This voids the last Auth transaction made. Please perform a Auth transaction first. <----")
                return@OnClickListener
            }
            addText("----> Starting Referenced Refund Transaction <----")
            val transaction = AnyPayTransaction()
            transaction.endpoint = endpoint
            transaction.externalId = refTransaction!!.externalId
            transaction.totalAmount = Amount("1")
            transaction.refTransactionId = refTransaction!!.externalId
            transaction.transactionType = TransactionType.REFUND

            //t.enableLogging();
            transaction.execute(object : TransactionListener {
                override fun onTransactionCompleted() {
                    dialogs.hideProgressDialog()
                    if (transaction.isApproved) addText("----> Transaction Refunded <----") else {
                        addText(transaction.responseText)
                    }
                }

                override fun onTransactionFailed(reason: MeaningfulError) {
                    dialogs.hideProgressDialog()
                    addText("Refund Failed")
                }
            })
        })
        btnSubmitSignature!!.setOnClickListener {
            dialogs.showProgressDialog(this@MainActivity, "Please Wait...")
            (refTransaction as CardTransaction?)!!.signature = signatureView!!.signature
            if (refTransaction!!.getCustomField("completed") != null) {
                (endpoint as WorldnetEndpoint?)!!.submitSignature(refTransaction, object : RequestListener<Any?> {
                    override fun onRequestComplete(o: Any?) {
                        addText("\r\n SIgnature Sent Successfully")
                    }

                    override fun onRequestFailed(meaningfulError: MeaningfulError) {
                        addText("\r\n SIgnature sent Failed")
                    }
                })
            } else {
                refTransaction!!.proceed()
            }
            addText("\r\nSending SIgnature")
        }
        authenticateTerminal()
    }

    private fun sendKeyedTransaction() {
        val transaction = AnyPayTransaction()
        transaction.endpoint = endpoint
        transaction.transactionType = TransactionType.SALE
        transaction.cardExpiryMonth = "10"
        transaction.cardExpiryYear = "20"
        transaction.address = "123 Main Street"
        transaction.postalCode = "30004"
        transaction.cvV2 = "999"
        transaction.cardholderName = "Jane Doe"
        transaction.cardNumber = "4012888888881881"
        transaction.totalAmount = Amount("10.47")
        transaction.currency = "USD"
        refTransaction = transaction
        //t.enableLogging();
        transaction.execute(transactionListener)
    }

    private fun sendEMVTransaction() {
        val transaction = AnyPayTransaction()
        transaction.endpoint = endpoint
        transaction.useCardReader(CardReaderController.getConnectedReader())
        transaction.transactionType = TransactionType.SALE
        transaction.address = "123 Main Street"
        transaction.postalCode = "30004"
        transaction.totalAmount = Amount("10.47")
        transaction.currency = "USD"
        transaction.setOnSignatureRequiredListener {
            addText("\r\n------>onSignatureRequired: sending null and proceeding")
            val signature = "base64-encoded image or point map"
            dialogs.hideProgressDialog()
            signatureViewLayout!!.visibility = View.VISIBLE
        }
        refTransaction = transaction

        //t.enableLogging();
        transaction.execute(object : CardTransactionListener {
            override fun onCardReaderEvent(event: MeaningfulMessage) {
                addText("""

    ------>onCardReaderEvent: $event
    """.trimIndent())
            }

            override fun onTransactionCompleted() {
                dialogs.hideProgressDialog()
                addText("""

    ------>onTransactionCompleted${transaction.isApproved}
    """.trimIndent())
                signatureViewLayout!!.visibility = View.GONE
            }

            override fun onTransactionFailed(reason: MeaningfulError) {
                dialogs.hideProgressDialog()
                addText("\r\n------>onTransactionFailed: $reason")
                signatureViewLayout!!.visibility = View.GONE
            }
        })
    }

    private fun addText(text: String) {
        txtPanel!!.append("""

    $text


    """.trimIndent())
    }

    private fun authenticateTerminal() {
        addText("Authenticating. Please Wait...")
        (endpoint as WorldnetEndpoint?)!!.worldnetSecret = ""
        (endpoint as WorldnetEndpoint?)!!.worldnetTerminalID = ""
        (endpoint as WorldnetEndpoint?)!!.url = "https://testpayments.anywherecommerce.com/merchant"
        (endpoint as WorldnetEndpoint?)!!.authenticate(object : AuthenticationListener {
            override fun onAuthenticationComplete() {
                addText("\r\n------> Terminal Authenticated")
            }

            override fun onAuthenticationFailed(meaningfulError: MeaningfulError) {
                addText("\r\n------> Terminal Authentication Failed")
            }
        })
    }

    private val transactionListener: TransactionListener = object : TransactionListener {
        override fun onTransactionCompleted() {
            dialogs.hideProgressDialog()
            addText("""

    ------>onTransactionCompleted - ${refTransaction!!.isApproved}
    """.trimIndent())
            if (refTransaction!!.transactionType == TransactionType.SALE) {
                val tip = TipLineItem()
                tip.amount = Amount("23")
                refTransaction!!.tip = tip
                (endpoint as WorldnetEndpoint?)!!.submitTipAdjustment(refTransaction, tip, object : RequestListener<Any?> {
                    override fun onRequestComplete(o: Any?) {
                        addText("\r\n------>Tip Adjustment success")
                    }

                    override fun onRequestFailed(meaningfulError: MeaningfulError) {
                        addText("\r\n------>Tip Adjustment failed")
                    }
                })

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

        override fun onTransactionFailed(reason: MeaningfulError) {
            dialogs.hideProgressDialog()
            addText("\r\n------>onTransactionFailed: $reason")
        }
    }

    companion object {
        var latch = false
    }
}