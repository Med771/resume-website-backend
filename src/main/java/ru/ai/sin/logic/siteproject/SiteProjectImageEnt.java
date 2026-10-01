package ru.ai.sin.logic.siteproject;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "site_project_images")
@Getter
@Setter
@NoArgsConstructor
public class SiteProjectImageEnt {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_project_id", nullable = false)
    private SiteProjectEnt project;

    @Column(name = "image_path", length = 512)
    private String imagePath;

    @Column(name = "image_url", length = 1024)
    private String imageUrl;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
