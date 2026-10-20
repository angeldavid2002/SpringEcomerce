package com.angel.ecomerce.entity;

import com.angel.ecomerce.entity.enums.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;



@Setter
@Getter
@Entity
@NoArgsConstructor
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String lastName;
    private String email;
    private String password;
    private boolean active;

    //
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
    //
    @CreationTimestamp
    private LocalDateTime created;
    //
    @UpdateTimestamp
    private LocalDateTime updated;
    //inventario
    //@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    //@JoinColumn(name = "inventory_id", nullable = true)
    //private Inventory inventory;
    @OneToOne(mappedBy = "user")
    private Inventory inventory;
}
