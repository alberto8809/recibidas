package org.example.createfile.util;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.example.createfile.model.Descripcion;
import org.example.createfile.model.PolicyObjFile;
import org.example.createfile.model.PolicyObjParser;
import org.example.createfile.model.Response;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParserFile {
    static Logger LOGGER = LogManager.getLogger(ParserFile.class);
    private static String bucketName = "xmlfilesback";


    public ParserFile() {
    }

    public static PolicyObjFile getParse(String path) {

        PolicyObjParser values = new PolicyObjParser();
        //String localPath;
        try {
            //localPath = createFile(FileName);
            File archivoXML = new File(path);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(archivoXML);
            doc.getDocumentElement().normalize();
            Element comprobanteElement = doc.getDocumentElement();

            Element total = doc.getDocumentElement();
            //"------------- total  ---------------
            values.setTotalAmount(total.getAttribute("Total"));

            Element date = doc.getDocumentElement();
            String currentDate = date.getAttribute("Fecha");


            Element payment = doc.getDocumentElement();
            String typeOfPayment = payment.getAttribute("FormaPago").isEmpty() || payment.getAttribute("FormaPago") == null ? "99" : payment.getAttribute("FormaPago");
            Element comprobante = doc.getDocumentElement();

            values.setType_of_value(comprobante.getAttribute("TipoDeComprobante"));

            Element method = doc.getDocumentElement();
            String methodPayment = method.getAttribute("MetodoPago");


            NodeList repectEgr = comprobanteElement.getElementsByTagName("cfdi:Receptor");
            Element rEgr = (Element) repectEgr.item(0);
            //System.out.println(" rEgr --- > "+rEgr.getAttribute("Nombre"));


            NodeList rfc = comprobanteElement.getElementsByTagName("cfdi:Emisor");
            Element RFC = (Element) rfc.item(0);
            values.setRfc(RFC.getAttribute("Rfc"));

            NodeList datoCli = comprobanteElement.getElementsByTagName("cfdi:Receptor");
            Element cliente = (Element) datoCli.item(0);
            //System.out.println(" cliente --- > "+cliente.getAttribute("Nombre"));
            String cli = cliente.getAttribute("Nombre");

            String typeOf = null;

            if (methodPayment.equals("PUE") && rEgr.getAttribute("Nombre").equals(cli)) {
                typeOf = "Egreso";
            } else if (methodPayment.equals("PPD") && rEgr.getAttribute("Nombre").equals(cli)) {
                typeOf = "Diario";
            }

            NodeList emisor = comprobanteElement.getElementsByTagName("cfdi:Emisor");
            Element rcp = (Element) emisor.item(0);
            String companyName = rcp.getAttribute("Nombre");

            if (comprobante.getAttribute("TipoDeComprobante").equals("I") && methodPayment.equals("PUE")) {

                values.setMetodo(methodPayment);

                NodeList impe = comprobanteElement.getElementsByTagName("cfdi:Impuestos");
                Element ime = (Element) impe.item(impe.getLength() - 1);
                values.setImpuestos(ime.getAttribute("TotalImpuestosTrasladados").isEmpty() ? "0" : ime.getAttribute("TotalImpuestosTrasladados"));


//                //"------------- Nombre de empresa ----------------
//                NodeList emisor = comprobanteElement.getElementsByTagName("cfdi:Emisor");
//                Element rcp = (Element) emisor.item(0);
//                companyName = rcp.getAttribute("Nombre");


                //"------------- RegimenFiscalReceptor ----------------
                NodeList receptor = comprobanteElement.getElementsByTagName("cfdi:Receptor");
                Element regimen = (Element) receptor.item(0);
                values.setRegimen(regimen.getAttribute("RegimenFiscalReceptor"));


                //"------------- UsoCFDI ----------------
                Element useCFDI = (Element) receptor.item(0);
                values.setUsoCFDI(useCFDI.getAttribute("UsoCFDI"));


                //"------------- Concepto ----------------
                NodeList conceptosList = comprobanteElement.getElementsByTagName("cfdi:Concepto");
                Element description = (Element) conceptosList.item(0);
                values.setConcepto_Descripcion(description.getAttribute("Descripcion"));

                //"------------- amount ----------------
                Element amount = (Element) conceptosList.item(0);
                values.setAmoubnt(amount.getAttribute("Importe"));


                //"------------- ClaveProdServ ----------------

                NodeList ClaveProdServ = comprobanteElement.getElementsByTagName("cfdi:Concepto");
                Element claveProdServ = (Element) ClaveProdServ.item(0);

                List<String> clv = new ArrayList<>();

                clv.add(claveProdServ.getAttribute("ClaveProdServ"));
                values.setClaveProdServ(clv);


                //"------------- Traslado ----------------
                NodeList traslados = comprobanteElement.getElementsByTagName("cfdi:Traslado");

                List<String> translado = new ArrayList<>();
                for (int i = 0; i < traslados.getLength(); i++) {
                    Element retencionR = (Element) traslados.item(i);
                    translado.add(retencionR.getAttribute("Importe").isEmpty() ? "0" : retencionR.getAttribute("Importe"));

                }

                values.setTraslado(translado);


                //"------------- Retencion ----------------
                NodeList retencion = comprobanteElement.getElementsByTagName("cfdi:Retencion");

                List<String> retencion_importe = new ArrayList<>();
                for (int i = 0; i < retencion.getLength(); i++) {
                    Element retencionR = (Element) retencion.item(i);
                    retencion_importe.add(retencionR.getAttribute("Importe"));
                }
                values.setRetencion_importe(retencion_importe);

                //"------------- TimbreFiscalDigital ----------------
                NodeList timbre = comprobanteElement.getElementsByTagName("tfd:TimbreFiscalDigital");
                Element uudi = (Element) timbre.item(0);
                values.setTimbreFiscalDigital_UUID(uudi.getAttribute("UUID"));


                values.setVenta_id("102.01");

                List<String> abono = new ArrayList<>();
                abono.add("401.01");
                abono.add("208.01");
                values.setAbono(abono);


            } else if (comprobante.getAttribute("TipoDeComprobante").equals("I") && methodPayment.equals("PPD")) {
                values.setMetodo(methodPayment);


                values.setRetencion_importe(new ArrayList<>());


                //"------------- RegimenFiscalReceptor ----------------
                NodeList receptor = comprobanteElement.getElementsByTagName("cfdi:Receptor");
                Element regimen = (Element) receptor.item(0);
                values.setRegimen(regimen.getAttribute("RegimenFiscalReceptor"));


                //"------------- UsoCFDI ----------------
                Element useCFDI = (Element) receptor.item(0);
                values.setUsoCFDI(useCFDI.getAttribute("UsoCFDI"));

                NodeList timbre = comprobanteElement.getElementsByTagName("tfd:TimbreFiscalDigital");
                Element uudi = (Element) timbre.item(0);
                values.setTimbreFiscalDigital_UUID(uudi.getAttribute("UUID"));


            } else if (comprobante.getAttribute("TipoDeComprobante").equals("E")) {
                System.out.println("inside parserFile");

                NodeList rcp2 = comprobanteElement.getElementsByTagName("cfdi:Receptor");
                Element rcp3 = (Element) rcp2.item(0);
                values.setMetodo(rcp3.getAttribute("Nombre"));


                values.setSubtotal(Double.parseDouble(comprobante.getAttribute("SubTotal")));


                NodeList impe = comprobanteElement.getElementsByTagName("cfdi:Impuestos");
                Element ime = (Element) impe.item(impe.getLength() - 1);
                values.setImpuestos(ime.getAttribute("TotalImpuestosTrasladados").isEmpty() ? "0" : ime.getAttribute("TotalImpuestosTrasladados"));


                //"------------- RegimenFiscalReceptor ----------------
                NodeList receptor = comprobanteElement.getElementsByTagName("cfdi:Receptor");
                Element regimen = (Element) receptor.item(0);
                values.setRegimen(regimen.getAttribute("RegimenFiscalReceptor"));


                //"------------- UsoCFDI ----------------
                Element useCFDI = (Element) receptor.item(0);
                values.setUsoCFDI(useCFDI.getAttribute("UsoCFDI"));


                //"------------- Concepto ----------------
                NodeList conceptosList = comprobanteElement.getElementsByTagName("cfdi:Concepto");
                Element description = (Element) conceptosList.item(0);
                values.setConcepto_Descripcion(description.getAttribute("Descripcion"));

                //"------------- amount ----------------
                Element amount = (Element) conceptosList.item(0);
                values.setAmoubnt(amount.getAttribute("Importe"));


                //"------------- ClaveProdServ ----------------

                NodeList ClaveProdServ = comprobanteElement.getElementsByTagName("cfdi:Concepto");
                Element claveProdServ = (Element) ClaveProdServ.item(0);

                List<String> clv = new ArrayList<>();

                clv.add(claveProdServ.getAttribute("ClaveProdServ"));
                values.setClaveProdServ(clv);


                //"------------- Traslado ----------------
                NodeList traslados = comprobanteElement.getElementsByTagName("cfdi:Traslado");

                List<String> translado = new ArrayList<>();
                for (int i = 0; i < traslados.getLength(); i++) {
                    Element retencionR = (Element) traslados.item(i);
                    translado.add(retencionR.getAttribute("Importe").isEmpty() ? "0" : retencionR.getAttribute("Importe"));

                }

                values.setTraslado(translado);


                //"------------- Retencion ----------------
                NodeList retencion = comprobanteElement.getElementsByTagName("cfdi:Concepto");
                List<String> retencion_importe = new ArrayList<>();
                for (int i = 0; i < retencion.getLength(); i++) {
                    Element retencionR = (Element) retencion.item(i);
                    retencion_importe.add(retencionR.getAttribute("Importe"));
                }
                values.setRetencion_importe(retencion_importe);

                //"------------- TimbreFiscalDigital ----------------
                NodeList timbre = comprobanteElement.getElementsByTagName("tfd:TimbreFiscalDigital");
                Element uudi = (Element) timbre.item(0);
                values.setTimbreFiscalDigital_UUID(uudi.getAttribute("UUID"));

            } else if (comprobante.getAttribute("TipoDeComprobante").equals("N")) {


                values.setMetodo(methodPayment);
                values.setSubtotal(Double.parseDouble(comprobante.getAttribute("SubTotal")));
//                NodeList impe = comprobanteElement.getElementsByTagName("cfdi:Impuestos");
//                Element ime = (Element) impe.item(impe.getLength() - 1);
                values.setImpuestos("0");


                //"------------- Nombre de empresa ----------------


                //"------------- RegimenFiscalReceptor ----------------
                NodeList receptor = comprobanteElement.getElementsByTagName("cfdi:Receptor");
                Element regimen = (Element) receptor.item(0);
                values.setRegimen(regimen.getAttribute("RegimenFiscalReceptor"));


                //"------------- UsoCFDI ----------------
                Element useCFDI = (Element) receptor.item(0);
                values.setUsoCFDI(useCFDI.getAttribute("UsoCFDI"));


                //"------------- Concepto ----------------
                NodeList conceptosList = comprobanteElement.getElementsByTagName("cfdi:Concepto");


                //"------------- amount ----------------
                Element amount = (Element) conceptosList.item(0);
                values.setAmoubnt(amount.getAttribute("Importe"));

                NodeList imp = comprobanteElement.getElementsByTagName("cfdi:Traslado");

                Map<String, String> iva = new HashMap<>();
                List<String> importte = new ArrayList<>();

                for (int j = 0; j < imp.getLength(); j++) {
                    Element clv = (Element) imp.item(j);
                    iva.put(clv.getAttribute("Impuesto"), clv.getAttribute("Importe"));
                }
                values.setIva(iva);


                for (String clv : iva.values()) {
                    importte.add(clv);
                }

                values.setTax_amount(importte);


                //"------------- ClaveProdServ ----------------


                NodeList tipo = comprobanteElement.getElementsByTagName("nomina12:Percepcion");
                List<String> claveProd = new ArrayList<>();

                for (int j = 0; j < tipo.getLength(); j++) {
                    Element clv = (Element) tipo.item(j);
                    claveProd.add(clv.getAttribute("TipoPercepcion"));
                }

                values.setClaveProdServ(claveProd);

                //"------------- Traslado ----------------
                NodeList traslados = comprobanteElement.getElementsByTagName("nomina12:Deduccion");

                List<String> translado = new ArrayList<>();
                for (int i = 0; i < traslados.getLength(); i++) {
                    Element retencionR = (Element) traslados.item(i);
                    translado.add(retencionR.getAttribute("Clave").isEmpty() ? "N/A" : retencionR.getAttribute("Clave"));

                }


                values.setTraslado(translado);

                //"------------- Retencion ----------------
                NodeList retencion = comprobanteElement.getElementsByTagName("nomina12:Deduccion");
                List<String> retencion_importe = new ArrayList<>();


                NodeList percep = comprobanteElement.getElementsByTagName("nomina12:Percepciones");
                Element totalSueldos = (Element) percep.item(0);

                retencion_importe.add(totalSueldos.getAttribute("TotalSueldos"));

                for (int i = 0; i < retencion.getLength(); i++) {
                    Element retencionR = (Element) retencion.item(i);
                    retencion_importe.add(retencionR.getAttribute("Importe"));
                }


                retencion_importe.add(values.getTotalAmount());
                values.setRetencion_importe(retencion_importe);


                //"------------- TimbreFiscalDigital ----------------
                NodeList timbre = comprobanteElement.getElementsByTagName("tfd:TimbreFiscalDigital");
                Element uudi = (Element) timbre.item(0);
                values.setTimbreFiscalDigital_UUID(uudi.getAttribute("UUID"));


            } else if (comprobante.getAttribute("TipoDeComprobante").equals("P")) {

                NodeList percep = comprobanteElement.getElementsByTagName("pago20:Totales");
                Element totalSueldos = (Element) percep.item(0);


                values.setMetodo(methodPayment);


                values.setSubtotal(Double.parseDouble(totalSueldos.getAttribute("TotalTrasladosImpuestoIVA16")));

                NodeList pago = comprobanteElement.getElementsByTagName("pago20:Pago");
                Element pay = (Element) pago.item(0);

                values.setImpuestos(pay.getAttribute("FormaDePagoP"));
                values.setUsoCFDI(pay.getAttribute("FechaPago").substring(0, 10));


                values.setType_of_value(pay.getAttribute("TipoCambioP"));
                values.setAmoubnt(pay.getAttribute("Monto"));

                NodeList docu = comprobanteElement.getElementsByTagName("pago20:DoctoRelacionado");
                Element d = (Element) docu.item(0);
                values.setTimbreFiscalDigital_UUID(d.getAttribute("IdDocumento") + " " + d.getAttribute("Folio"));
                values.setConcepto_Descripcion(d.getAttribute("MonedaDR"));
                values.setTotalAmount(d.getAttribute("ImpSaldoAnt"));

                List<String> clv = new ArrayList<>();

                clv.add(d.getAttribute("ImpPagado"));
                values.setClaveProdServ(clv);

                values.setRegimen(d.getAttribute("NumParcialidad"));


                //"------------- Nombre de empresa ----------------
                NodeList emisor2 = comprobanteElement.getElementsByTagName("cfdi:Emisor");
                Element rcp2 = (Element) emisor2.item(0);
                companyName = rcp2.getAttribute("Nombre");
                values.setMetodo("P");

                values.setRetencion_importe(new ArrayList<>());


                return new PolicyObjFile(values, path, companyName, cli, currentDate.substring(0, 10), typeOf, typeOfPayment);


            }


            //"------------- Return the object  ----------------
            //FileUtils.deleteDirectory(new File(localPath.substring(0, 4)));
            return new PolicyObjFile(values, path, companyName, cli, currentDate.substring(0, 10), typeOf, typeOfPayment);


        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    public static Response getParseValues(String localPath) {

        Response response = new Response();
        Descripcion descripcion = new Descripcion();

        try {

            //LOGGER.info("path from AWS:  { " + path + " }");
            File archivoXML = new File(localPath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(archivoXML);
            doc.getDocumentElement().normalize();
            Element comprobanteElement = doc.getDocumentElement();


            /* get client from xml*/
            NodeList repectEgr = comprobanteElement.getElementsByTagName("cfdi:Receptor");
            Element cliente = (Element) repectEgr.item(0);
            descripcion.setCliente(cliente.getAttribute("Nombre"));

            /* get amount from xml*/
            Element total = doc.getDocumentElement();
            descripcion.setCantidad(total.getAttribute("Total"));

            /* get date from xml*/
            Element date = doc.getDocumentElement();
            descripcion.setFecha(date.getAttribute("Fecha").substring(0, 10));
            response.setDescripcion(descripcion);

            archivoXML.delete();

        } catch (Exception e) {
            LOGGER.error("error { " + e.getLocalizedMessage() + " }");
        }
        return response;
    }

}

