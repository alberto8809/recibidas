package org.example.createfile.service;


import org.example.createfile.model.CuentaContable;
import org.example.createfile.model.PolicyObjFile;
import org.example.createfile.model.Regimen;
import org.example.createfile.repository.*;
import org.example.createfile.util.CreateFilePDF;
import org.example.createfile.util.ParserFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.*;


@Service

public class CreateFileService {
    private static String local_path = "/Users/marioalberto/IdeaProjects/polilzas_emitidas2/";

    @Autowired
    CreateFileRepository createFileRepository;
    @Autowired
    ClaveProductoServRepository claveProductoServRepository;
    @Autowired
    CuentaContableRepository cuentaContableRepository;
    @Autowired
    CFDIRepository cfdiRepository;
    @Autowired
    MethodOfPaymentRepository methodOfPaymentRepository;


    public CreateFileService() {
    }

    public CreateFileService(CreateFileRepository createFileRepository, ClaveProductoServRepository claveProductoServRepository, CuentaContableRepository cuentaContableRepository, CFDIRepository cfdiRepository, MethodOfPaymentRepository methodOfPaymentRepository) {
        this.createFileRepository = createFileRepository;
        this.claveProductoServRepository = claveProductoServRepository;
        this.cuentaContableRepository = cuentaContableRepository;
        this.cfdiRepository = cfdiRepository;
        this.methodOfPaymentRepository = methodOfPaymentRepository;
        //   this.policyRepository = policyRepository;
    }

    public Regimen getRegimens(String regimen) {
        return createFileRepository.getRegimen(regimen);
    }


    public List<String> getClaveProductoService(List<String> c_claveprodserv, String type, List<String> nomine) {
        List<String> claveProductoServs = new ArrayList<>();
        if (c_claveprodserv != null || !c_claveprodserv.isEmpty()) {
            for (String clv : c_claveprodserv) {

                if (type.equals("E")) {
                    claveProductoServs.add(claveProductoServRepository.getClaveProductoS("01010101"));
                } else if (type.equals("N")) {
                    //query to get accounts
                    claveProductoServs.add(methodOfPaymentRepository.getCuentaContablePer(clv));
                    for (String value : nomine) {
                        claveProductoServs.add(methodOfPaymentRepository.getCuentaContableByNomina(value));

                    }

                } else {
                    claveProductoServs.add(claveProductoServRepository.getClaveProductoS(clv));

                }
            }
        }
        return claveProductoServs;
    }


    public List<String> getCuentaCobtableList(List<String> claveProductoServ) {
        List<String> account = new ArrayList<>();

        for (String clv : claveProductoServ) {
            account.add(cuentaContableRepository.getCuantaContableMethod(clv));
        }

        return account;
    }

    public List<String> getIvaIeps(Map<String, String> claveProductoServ, String type, String amount) {
        List<String> iva = new ArrayList<>();

        if (type.equals("P")) {
            System.out.println("here");
            for (String clv : claveProductoServ.keySet()) {
                iva.add(methodOfPaymentRepository.getCuentaContableByTax(clv));
            }
        } else {

            for (String clv : claveProductoServ.keySet()) {
                iva.add(methodOfPaymentRepository.getCuentaContableByTax(clv));
            }
        }
        return iva;
    }




    public boolean createFile(MultipartFile[] files, String rfc) throws IOException {


        //make logic to get all values and then pass to front
        UUID uuid = UUID.randomUUID();
        List<CuentaContable> cuentaContable = new ArrayList<>();

        System.out.println("File .... " + files);
        //check all the paths
        PolicyObjFile policyObjFile = ParserFile.getParse(files);

        System.out.println(policyObjFile);


        if (policyObjFile.getPolicyObj().getMetodo().equals("P")) {

            policyObjFile.setCuenta_method("102.01");
            policyObjFile.setDescription_methods(cuentaContableRepository.getCuentaContableVenta(policyObjFile.getCuenta_method()));


            List<String> id = new ArrayList<>();
            id.add("209.01");
            id.add("105.99");
            policyObjFile.setTax_id(id);

            List<String> desc = new ArrayList<>();
            desc.add(cuentaContableRepository.getCuentaContableVenta(policyObjFile.getTax_id().get(0)));
            desc.add(cuentaContableRepository.getCuentaContableVenta(policyObjFile.getTax_id().get(1)));

            policyObjFile.setTax_description(desc);


            policyObjFile.getPolicyObj().setVenta_id("208.01");
            policyObjFile.getPolicyObj().setVenta_descripcion(cuentaContableRepository.getCuentaContableVenta(policyObjFile.getPolicyObj().getVenta_id()));


            policyObjFile.setFolio(uuid + " - " + policyObjFile.getPolicyObj().getRfc());
            //guardar en DB

            CreateFilePDF.makeFile(policyObjFile, policyObjFile.getPolicyObj().getRfc() + policyObjFile.getDate().substring(0, 7), new ArrayList<>());
            return true;
        } else if (policyObjFile.getPolicyObj().getType_of_value().equals("I") && policyObjFile.getPolicyObj().getMetodo().equals("PUE")) {

            List<String> claveProductoServ = getClaveProductoService(policyObjFile.getPolicyObj().getClaveProdServ(), policyObjFile.getPolicyObj().getType_of_value(), policyObjFile.getPolicyObj().getTraslado());
            cuentaContable.add(cuentaContableRepository.getCuantaContable(policyObjFile.getPolicyObj().getVenta_id()));
            policyObjFile.getPolicyObj().setVenta_descripcion(cuentaContableRepository.getCuentaContableVenta(policyObjFile.getPolicyObj().getVenta_id()));
            cuentaContable.add(cuentaContableRepository.getCuantaContable(claveProductoServ.isEmpty() || claveProductoServ == null ? "01" : claveProductoServ.get(0)));


            policyObjFile.setCuenta_method("216.04");
            policyObjFile.setDescription_methods(cuentaContableRepository.getCuentaContableVenta(policyObjFile.getCuenta_method()));

            // (Cargo)

            List<String> id = new ArrayList<>();
            id.add("216.10");
            policyObjFile.setTax_id(id);

            List<String> desc = new ArrayList<>();
            desc.add(cuentaContableRepository.getCuentaContableVenta(policyObjFile.getTax_id().get(0)));

            policyObjFile.setTax_description(desc);

            List<String> abonos = policyObjFile.getPolicyObj().getAbono();
            List<String> cargo = new ArrayList<>();
            for (String clv : abonos) {
                cargo.add(cuentaContableRepository.getCuantaContableMethod(clv));
            }
            policyObjFile.getPolicyObj().setCargo(cargo);


            policyObjFile.setFolio(uuid + " - " + policyObjFile.getPolicyObj().getRfc());

            CreateFilePDF.makeFile(policyObjFile, policyObjFile.getPolicyObj().getRfc() + policyObjFile.getDate().substring(0, 7), cuentaContable);
            return true;
        } else if (policyObjFile.getPolicyObj().getType_of_value().equals("I") && policyObjFile.getPolicyObj().getMetodo().equals("PPD")) {

            policyObjFile.setCuenta_method("401.07");
            policyObjFile.setDescription_methods(cuentaContableRepository.getCuentaContableVenta(policyObjFile.getCuenta_method()));

            List<String> id = new ArrayList<>();
            id.add("205.99");
            policyObjFile.setTax_id(id);

            List<String> desc = new ArrayList<>();
            desc.add(cuentaContableRepository.getCuentaContableVenta(policyObjFile.getTax_id().get(0)));

            policyObjFile.setTax_description(desc);


            policyObjFile.getPolicyObj().setVenta_id("209.01");
            policyObjFile.getPolicyObj().setVenta_descripcion(cuentaContableRepository.getCuentaContableVenta(policyObjFile.getPolicyObj().getVenta_id()));


            policyObjFile.setFolio(uuid + " - " + policyObjFile.getPolicyObj().getRfc());

            CreateFilePDF.makeFile(policyObjFile, policyObjFile.getPolicyObj().getRfc() + policyObjFile.getDate().substring(0, 7), cuentaContable);
            return true;

        } else if (policyObjFile.getPolicyObj().getType_of_value().equals("E")) {
            System.out.println("in");

            policyObjFile.setCuenta_method("402.01");
            policyObjFile.setDescription_methods(cuentaContableRepository.getCuentaContableVenta(policyObjFile.getCuenta_method()));


            List<String> id = new ArrayList<>();
            id.add("105.99");
            policyObjFile.setTax_id(id);


            List<String> desc = new ArrayList<>();
            desc.add(cuentaContableRepository.getCuentaContableVenta(policyObjFile.getTax_id().get(0)));

            policyObjFile.setTax_description(desc);


            policyObjFile.getPolicyObj().setVenta_id("209.01");
            policyObjFile.getPolicyObj().setVenta_descripcion(cuentaContableRepository.getCuentaContableVenta(policyObjFile.getPolicyObj().getVenta_id()));

            policyObjFile.setFolio(uuid + " - " + policyObjFile.getPolicyObj().getRfc());

            CreateFilePDF.makeFile(policyObjFile, policyObjFile.getPolicyObj().getRfc() + policyObjFile.getDate().substring(0, 7), cuentaContable);

            return true;

        } else if (policyObjFile.getPolicyObj().getType_of_value().equals("N")) {

            List<String> claveProductoServ = getClaveProductoService(policyObjFile.getPolicyObj().getClaveProdServ(), policyObjFile.getPolicyObj().getType_of_value(), policyObjFile.getPolicyObj().getTraslado());
            List<String> accounts = getCuentaCobtableList(claveProductoServ);

            //method payment (Abono)
            policyObjFile.setCuenta_method(methodOfPaymentRepository.getCuentaContable(policyObjFile.getTypeOfPayment()));
            policyObjFile.setDescription_methods(cuentaContableRepository.getCuantaContableMethod(policyObjFile.getCuenta_method()));

            claveProductoServ.add(policyObjFile.getCuenta_method());
            accounts.add(policyObjFile.getDescription_methods());
            policyObjFile.setTax_id(claveProductoServ);
            policyObjFile.setTax_description(accounts);

            policyObjFile.setFolio(uuid + " - " + policyObjFile.getPolicyObj().getRfc());
            System.out.println(policyObjFile);
            //save policy in DB
            // savePolicy(policyObjFile);
            CreateFilePDF.makeFile(policyObjFile, policyObjFile.getPolicyObj().getRfc() + policyObjFile.getDate().substring(0, 7), cuentaContable);

        } else {
            // (Cargo)
            policyObjFile.setTax_id(getIvaIeps(policyObjFile.getPolicyObj().getIva(), policyObjFile.getPolicyObj().getType_of_value(), policyObjFile.getPolicyObj().getAmoubnt()));
            policyObjFile.setTax_description(getCuentaCobtableList(policyObjFile.getTax_id()));
            policyObjFile.setFolio(uuid + " - " + policyObjFile.getPolicyObj().getRfc());
            CreateFilePDF.makeFile(policyObjFile, policyObjFile.getPolicyObj().getRfc() + policyObjFile.getDate().substring(0, 7), cuentaContable);

        }

//        storeFile(files, rfc, "2022-02");
        //UploadFileToS3.getFilelFromAWS(rfc)
        return false;

    }

}


