package org.example.createfile.repository;

import org.example.createfile.model.ClaveProductoServ;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ClaveProductoServRepository extends JpaRepository<ClaveProductoServ,String> {
    @Query(value = "SELECT *  FROM dbmaster.claveProdServ cp WHERE cp.c_claveprodserv=:c_claveprodserv " ,nativeQuery = true)
    org.example.createfile.model.ClaveProductoServ getClaveProducto(String c_claveprodserv);

    @Query(value = "SELECT *  FROM dbmaster.claveProdServ cp WHERE cp.c_claveprodserv=:c_claveprodserv " ,nativeQuery = true)
    String getClaveProductoS(String c_claveprodserv);



}
