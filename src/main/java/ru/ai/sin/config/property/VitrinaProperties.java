package ru.ai.sin.config.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.vitrina.home")
public class VitrinaProperties {

    private int studentsLimit = 24;

    private int projectsLimit = 6;
}
