package ru.ai.sin.logic.company;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.ai.sin.helper.SecurityHelper;
import ru.ai.sin.logic.company.dto.AddCompanyReq;
import ru.ai.sin.logic.company.dto.CompanyDTO;
import ru.ai.sin.logic.company.dto.UpdateCompanyReq;
import ru.ai.sin.tools.CompanyTools;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyWebsiteTest {

    @Mock
    private CompanyRepo companyRepo;
    @Mock
    private CompanyTools companyTools;
    @Mock
    private SecurityHelper securityHelper;

    @InjectMocks
    private CompanyServiceImpl companyService;

    @Test
    void create_withoutWebsite_returnsNull() {
        when(companyRepo.save(any(CompanyEnt.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(companyTools.newObjMapToDTO(any(CompanyEnt.class))).thenAnswer(invocation -> dto(invocation.getArgument(0)));
        when(securityHelper.getCurrentUsername()).thenReturn("admin");

        CompanyDTO result = companyService.create(new AddCompanyReq("Acme", null));

        assertThat(result.website()).isNull();
    }

    @Test
    void create_withWebsite_returnsIt() {
        when(companyRepo.save(any(CompanyEnt.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(companyTools.newObjMapToDTO(any(CompanyEnt.class))).thenAnswer(invocation -> dto(invocation.getArgument(0)));
        when(securityHelper.getCurrentUsername()).thenReturn("admin");

        CompanyDTO result = companyService.create(new AddCompanyReq("Acme", " https://acme.example "));

        assertThat(result.website()).isEqualTo("https://acme.example");
    }

    @Test
    void update_withoutWebsite_keepsExistingLink() {
        CompanyEnt company = new CompanyEnt("Acme");
        company.setWebsite("https://old.example");
        when(companyTools.getCompanyOrThrow(1L)).thenReturn(company);
        when(companyTools.mapToDTO(company)).thenAnswer(invocation -> dto(company));
        when(securityHelper.getCurrentUsername()).thenReturn("admin");

        CompanyDTO result = companyService.updateById(1L, new UpdateCompanyReq("New name", null));

        assertThat(result.name()).isEqualTo("New name");
        assertThat(result.website()).isEqualTo("https://old.example");
    }

    @Test
    void update_withWebsite_replacesLink() {
        CompanyEnt company = new CompanyEnt("Acme");
        company.setWebsite("https://old.example");
        when(companyTools.getCompanyOrThrow(1L)).thenReturn(company);
        when(companyTools.mapToDTO(company)).thenAnswer(invocation -> dto(company));
        when(securityHelper.getCurrentUsername()).thenReturn("admin");

        CompanyDTO result = companyService.updateById(1L, new UpdateCompanyReq("Acme", "https://new.example"));

        assertThat(result.website()).isEqualTo("https://new.example");
        ArgumentCaptor<CompanyEnt> saved = ArgumentCaptor.forClass(CompanyEnt.class);
        verify(companyTools).mapToDTO(saved.capture());
        assertThat(saved.getValue().getWebsite()).isEqualTo("https://new.example");
    }

    private static CompanyDTO dto(CompanyEnt company) {
        return new CompanyDTO(1L, company.getName(), company.getWebsite(), List.of());
    }
}
