package com.anywherecommerce.android.sdk.sampleapp

import android.app.Activity
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.anywherecommerce.android.sdk.*
import com.anywherecommerce.android.sdk.AppBackgroundingManager.AppBackroundedListener
import com.anywherecommerce.android.sdk.devices.CardReader
import com.anywherecommerce.android.sdk.devices.CardReaderController
import com.anywherecommerce.android.sdk.devices.bbpos.BBPOSDevice
import com.anywherecommerce.android.sdk.endpoints.AnyPayTransaction
import com.anywherecommerce.android.sdk.endpoints.propay.PropayEndpoint
import com.anywherecommerce.android.sdk.endpoints.propay.PropayJsonGatewayResponse
import com.anywherecommerce.android.sdk.models.TransactionType
import com.anywherecommerce.android.sdk.sampleapp.ProPayActivity
import com.anywherecommerce.android.sdk.transactions.Transaction
import com.anywherecommerce.android.sdk.transactions.listener.CardTransactionListener
import com.anywherecommerce.android.sdk.transactions.listener.TransactionListener
import com.anywherecommerce.android.sdk.util.Amount
import com.bbpos.bbdevice.BBDeviceController

class ProPayActivity : Activity() {
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
    var emvAuth: Button? = null
    var dialogs = DialogManager()
    var refTransaction: Transaction? = null
    var endpoint: Endpoint? = null
    var propaySessionID: String? = null
    override fun onCreate(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pro_pay)
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
        btnCaptureTransaction = findViewById<View>(R.id.btnCapture) as Button
        btnRefRefund = findViewById<View>(R.id.refRefund) as Button
        txtReferenceId = findViewById<View>(R.id.txtReferenceId) as EditText
        btUSBConnect = findViewById<View>(R.id.btUSBConnect) as Button
        emvAuth = findViewById<View>(R.id.emvAuth) as Button
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
            dialogs.showProgressDialog(this@ProPayActivity, "Please Wait...")
            sendEMVTransaction(propaySessionID)
        })
        emvAuth!!.setOnClickListener(View.OnClickListener {
            addText("\nStarting EMV Auth transaction")
            if (!CardReaderController.isCardReaderConnected()) {
                addText("\r\nNo card reader connected")
                return@OnClickListener
            }
            dialogs.showProgressDialog(this@ProPayActivity, "Please Wait...")
            sendEMVAuthTransaction(propaySessionID)
        })
        btnTerminalLogin!!.setOnClickListener { authenticateTerminal() }
        btnKeyedsale!!.setOnClickListener {
            addText("\r\nExecuting Keyed Sale Transaction. Please Wait...")
            dialogs.showProgressDialog(this@ProPayActivity, "Please Wait...")

            //For ProPay
            sendKeyedTransaction(propaySessionID)
        }
        btnCaptureTransaction!!.setOnClickListener(View.OnClickListener { //For ProPay
            if (refTransaction == null) {
                addText("----> This captures the last Auth transaction made. Please perform a Auth transaction first. <----")
                return@OnClickListener
            }
            addText("----> Performing capture on last Transaction <----")
            dialogs.showProgressDialog(this@ProPayActivity, "Please Wait...")
            val captureT = refTransaction!!.createCapture() as AnyPayTransaction
            captureT.endpoint = refTransaction!!.endpoint
            captureT.execute(object : TransactionListener {
                override fun onTransactionCompleted() {
                    dialogs.hideProgressDialog()
                    if (captureT.isApproved) addText("----> Transaction Captured <----") else {
                        addText(captureT.gatewayResponse.status)
                    }
                }

                override fun onTransactionFailed(reason: MeaningfulError) {
                    addText("Capture Failed " + reason)
                }
            })
        })
        btnRefRefund!!.setOnClickListener(View.OnClickListener {
            if (refTransaction == null) {
                addText("----> This voids the last Auth transaction made. Please perform a Auth transaction first. <----")
                return@OnClickListener
            }
            addText("----> Starting Referenced Refund Transaction <----")
            val transaction = AnyPayTransaction()
            transaction.endpoint = endpoint
            transaction.externalId = refTransaction!!.externalId
            transaction.totalAmount = refTransaction!!.totalAmount
            transaction.refTransactionId = refTransaction!!.externalId
            transaction.transactionType = TransactionType.VOID

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
        authenticateTerminal()
    }

    //For ProPay
    private fun sendKeyedTransaction(sessionToken: String?) {
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
        transaction.addCustomField("sessionToken", sessionToken)
        refTransaction = transaction
        //t.enableLogging();
        transaction.execute(transactionListener)
    }

    private fun sendEMVTransaction(sessionToken: String?) {
        val transaction = AnyPayTransaction()
        transaction.endpoint = endpoint
        transaction.useCardReader(CardReaderController.getConnectedReader())
        transaction.transactionType = TransactionType.SALE
        transaction.address = "123 Main Street"
        transaction.postalCode = "30004"
        transaction.totalAmount = Amount("100")
        transaction.currency = "USD"
        transaction.addCustomField("sessionToken", sessionToken)
        refTransaction = transaction
        transaction.setOnSignatureRequiredListener {
            addText("\r\n------>On SIgnature Requires")
            transaction.proceed()
        }
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
            }

            override fun onTransactionFailed(reason: MeaningfulError) {
                dialogs.hideProgressDialog()
                addText("\r\n------>onTransactionFailed: $reason")
            }
        })
    }

    private fun sendEMVAuthTransaction(sessionToken: String?) {
        val transaction = AnyPayTransaction()
        transaction.endpoint = endpoint
        transaction.useCardReader(CardReaderController.getConnectedReader())
        transaction.transactionType = TransactionType.AUTHONLY
        transaction.address = "123 Main Street"
        transaction.postalCode = "30004"
        transaction.totalAmount = Amount("200")
        transaction.currency = "USD"
        transaction.addCustomField("sessionToken", sessionToken)
        refTransaction = transaction
        transaction.setOnSignatureRequiredListener {
            addText("\r\n------>On SIgnature Requires")
            transaction.proceed()
        }
        transaction.execute(object : CardTransactionListener {
            override fun onCardReaderEvent(event: MeaningfulMessage) {
                addText(("""
        
            ------>onCardReaderEvent: """ + event + """
            """).trimIndent())
            }

            override fun onTransactionCompleted() {
                dialogs.hideProgressDialog()
                addText("""

    ------>onTransactionCompleted${transaction.isApproved}
    """.trimIndent())
            }

            override fun onTransactionFailed(reason: MeaningfulError) {
                dialogs.hideProgressDialog()
                addText("\r\n------>onTransactionFailed: $reason")
            }
        })
    }

    private fun addText(text: String) {
        txtPanel!!.append("""

    $text


    """.trimIndent())
    }

    private fun authenticateTerminal() {
        addText("Getting Session ID. Please Wait...")

        //For ProPay
        (endpoint as PropayEndpoint?)!!.certStr = ""
        (endpoint as PropayEndpoint?)!!.x509Cert = ""
        (endpoint as PropayEndpoint?)!!.xmlApiBaseUrl = "https://xmltest.propay.com/api/"
        (endpoint as PropayEndpoint?)!!.jsonApiBaseUrl = "https://il01mobileapi.propay.com/merchant.svc/json/"
        (endpoint as PropayEndpoint?)!!.accountNum = ""
        (endpoint as PropayEndpoint?)!!.terminalId = ""

//        Terminal.getInstance().getConfiguration().setProperty("endpoint", endpoint);
//        Terminal.getInstance().saveState();
//        Terminal.restoreState();
        (endpoint as PropayEndpoint?)!!.getSessionId(object : RequestListener<String> {
            override fun onRequestComplete(response: String) {
                addText("\r\n------>Propay SessionID: $response")
                propaySessionID = response
            }

            override fun onRequestFailed(reason: MeaningfulError) {
                addText("\r\n------>Error retrieving SessionID: $reason")
            }
        })
    }

    private val transactionListener: TransactionListener = object : TransactionListener {
        override fun onTransactionCompleted() {
            dialogs.hideProgressDialog()
            addText("""

    ------>onTransactionCompleted - ${(refTransaction!!.gatewayResponse as PropayJsonGatewayResponse).responseJson}
    """.trimIndent())
        }

        override fun onTransactionFailed(reason: MeaningfulError) {
            dialogs.hideProgressDialog()
            addText("\r\n------>onTransactionFailed: $reason")
        }
    }

    override fun onPause() {
        super.onPause()
        if (refTransaction != null && refTransaction!!.isExecuting) {
            refTransaction!!.cancel()
        }
    }

    companion object {
        var latch = false
    }
}