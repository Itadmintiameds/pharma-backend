package tiameds.pharmabackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "pharma_documents")
public class PharmaDocuments {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_id")
    private Long documentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pharmacy_id", referencedColumnName = "pharmacy_id")
    private PharmacyDetails pharmacy;

    @Column(name = "document_no")
    private String documentNo;

    @Column(name = "document_type")
    private String documentType;

    @Column(name = "document_url")
    private String documentUrl;

    @Column(name = "issue_date")
    private LocalDateTime issueDate;

    @Column(name = "issue_authority")
    private String issueAuthority;

    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    @Column(name = "is_active")
    private Boolean isActive;
}
