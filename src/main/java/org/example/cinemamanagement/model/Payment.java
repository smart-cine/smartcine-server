package org.example.cinemamanagement.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.internal.bind.JsonAdapterAnnotationTypeAdapterFactory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.cinemamanagement.common.BankType;
import org.example.cinemamanagement.common.Status;
import org.example.cinemamanagement.mapper.JpaConverterJson;
import org.example.cinemamanagement.mapper.JsonToMapConverter;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payment")
public class Payment {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.DETACH,
            CascadeType.MERGE,
            CascadeType.PERSIST,
            CascadeType.REFRESH})
    @JoinColumn(name = "account_id")
    private Account account;

    @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.DETACH,
            CascadeType.MERGE,
            CascadeType.PERSIST,
            CascadeType.REFRESH})
    @JoinColumn(name = "perform_id")
    private Perform perform;


    @Column(name = "date_created")
    private Timestamp dateCreated;

    @Column(name = "date_expired")
    private Timestamp dateExpired;


    @JsonIgnore
    @ManyToMany(mappedBy = "payments", cascade = {CascadeType.DETACH,
            CascadeType.MERGE,
            CascadeType.PERSIST,
            CascadeType.REFRESH}, fetch = FetchType.LAZY)
    private List<Item> item;


    @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.DETACH,
            CascadeType.MERGE,
            CascadeType.PERSIST,
            CascadeType.REFRESH})
    @JoinColumn(name = "business_bank_id")
    private BusinessBank businessBank;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private BankType bankType;

    // json type
    @Column(name = "data", columnDefinition = "json")
    @Convert(converter = JpaConverterJson.class)
    private Map<String, Object> data;
}
