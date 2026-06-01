package com.nqt.identity_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.UuidGenerator;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClientProfile  implements Serializable {
    @Id
    @UuidGenerator
    @Column(name = "client_profile_id")
    UUID id;

    String companyName;
    String industry;
    String companyDescription;

    @OneToOne(orphanRemoval = true)
    @JoinColumn(name = "user_id")
    User user;

}
