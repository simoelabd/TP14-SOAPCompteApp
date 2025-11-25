package ma.projet.soapcompteapp.ws

import ma.projet.soapcompteapp.beans.Compte
import ma.projet.soapcompteapp.beans.TypeCompte
import org.ksoap2.SoapEnvelope
import org.ksoap2.serialization.SoapObject
import org.ksoap2.serialization.SoapSerializationEnvelope
import org.ksoap2.transport.HttpTransportSE
import java.text.SimpleDateFormat
import java.util.*

class Service {


    private val NAMESPACE = "http://ws.tp13.example.com/"
    private val URL = "http://10.0.2.2:8080/services/ws"

    private val METHOD_GET_COMPTES = "getComptes"
    private val METHOD_CREATE_COMPTE = "createCompte"
    private val METHOD_DELETE_COMPTE = "deleteCompte"

    // Formatter principal pour xs:dateTime
    private val dateTimeFormatter =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

    // Formatter de secours si jamais c’est juste yyyy-MM-dd
    private val dateFormatterFallback =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /**
     * Récupère tous les comptes via le service SOAP.
     */
    fun getComptes(): List<Compte> {
        val comptes = mutableListOf<Compte>()

        val request = SoapObject(NAMESPACE, METHOD_GET_COMPTES)
        val envelope = SoapSerializationEnvelope(SoapEnvelope.VER11).apply {
            dotNet = false
            setOutputSoapObject(request)
        }
        val transport = HttpTransportSE(URL)

        try {
            transport.call("", envelope)
            val body = envelope.bodyIn as SoapObject
            // getComptesResponse contient en général plusieurs éléments "return"
            for (i in 0 until body.propertyCount) {
                val soapCompte = body.getProperty(i) as SoapObject

                val idStr = soapCompte.getPropertySafelyAsString("id")
                val soldeStr = soapCompte.getPropertySafelyAsString("solde")
                val dateStr = soapCompte.getPropertySafelyAsString("dateCreation")
                val typeStr = soapCompte.getPropertySafelyAsString("type")

                // Parse robuste de la date
                val parsedDate: Date = try {
                    // ex: 2025-11-16T11:06:25
                    dateTimeFormatter.parse(dateStr)
                } catch (e: Exception) {
                    try {
                        // ex: 2025-11-16
                        dateFormatterFallback.parse(dateStr)
                    } catch (e2: Exception) {
                        Date()
                    }
                } ?: Date()

                val compte = Compte(
                    id = idStr.toLongOrNull(),
                    solde = soldeStr.toDoubleOrNull() ?: 0.0,
                    dateCreation = parsedDate,
                    type = TypeCompte.valueOf(typeStr)
                )

                comptes.add(compte)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return comptes
    }

    /**
     * Crée un nouveau compte via le service SOAP.
     */
    fun createCompte(solde: Double, type: TypeCompte): Boolean {
        val request = SoapObject(NAMESPACE, METHOD_CREATE_COMPTE).apply {

            addProperty("solde", solde.toString())
            addProperty("type", type.name) // COURANT ou EPARGNE
        }

        val envelope = SoapSerializationEnvelope(SoapEnvelope.VER11).apply {
            dotNet = false
            setOutputSoapObject(request)
        }
        val transport = HttpTransportSE(URL)

        return try {
            transport.call("", envelope)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Supprime un compte par ID.
     */
    fun deleteCompte(id: Long): Boolean {
        val request = SoapObject(NAMESPACE, METHOD_DELETE_COMPTE).apply {
            addProperty("id", id)
        }

        val envelope = SoapSerializationEnvelope(SoapEnvelope.VER11).apply {
            dotNet = false
            setOutputSoapObject(request)
        }
        val transport = HttpTransportSE(URL)

        return try {
            transport.call("", envelope)
            // si ton WS retourne un boolean
            (envelope.response as? Boolean) ?: true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}