package com.cse586.lab1.model;

import javax.persistence.*;
import java.sql.Date;

@Entity
public class Waypoint {
    @Column(name = "DATE", nullable = false)
    private Date date;

    @Id
    @Column(name = "ROUTE", nullable = false)
    private String route;

    @Lob
    @Column(name = "METADATA", nullable = false)
    private String metadata;


    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
}
