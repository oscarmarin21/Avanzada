package com.avanzada.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "app_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String identifier;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false)
    private Boolean active;

    @Column(length = 50)
    private String role;
}
