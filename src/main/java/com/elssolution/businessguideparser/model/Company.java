package com.elssolution.businessguideparser.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Company {
    private String companyName; // Потрібно буде знайти, де взяти назву компанії (ймовірно з тегу <title> або <h1>)
    private String address;
    private String postalAddress;
    private String phone;
    private String fax; // Якщо потрібно
    private String contactPerson;
    private String director;
    private String accountant;
    private String accountantPhone;
    private String registrationNumber;
    private String foundationYear;
    private String employeeCount;
    private String tin; // ІПН
    private String certificateNumber; // Номер свідоцтва
    private String website;
    private String emailLink; // Посилання для відправки email (якщо сам email не доступний)
    private String sourceUrl; // URL сторінки, з якої взяті дані
    private String description;
}
