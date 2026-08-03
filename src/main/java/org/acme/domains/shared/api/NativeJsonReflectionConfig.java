package org.acme.domains.shared.api;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.acme.domains.auth.dto.LoginContextData;
import org.acme.domains.auth.dto.LoginRequest;
import org.acme.domains.auth.dto.LoginResponse;
import org.acme.domains.auth.dto.SwitchCompanyRequest;
import org.acme.domains.benefit.dto.BenefitResponse;
import org.acme.domains.benefit.dto.CreateBenefitRequest;
import org.acme.domains.benefit.dto.UpdateBenefitRequest;
import org.acme.domains.benefitrequest.dto.BenefitAccessRequestResponse;
import org.acme.domains.benefitrequest.dto.CreateBenefitAccessRequest;
import org.acme.domains.benefitrequest.dto.RejectBenefitAccessRequest;
import org.acme.domains.category.dto.CategoryResponse;
import org.acme.domains.company.dto.CompanyResponse;
import org.acme.domains.company.dto.CreateCompanyRequest;
import org.acme.domains.company.dto.DeactivateCompanyRequest;
import org.acme.domains.company.dto.UpdateCompanyRequest;
import org.acme.domains.employee.dto.CreateEmployeeRequest;
import org.acme.domains.employee.dto.EmployeeResponse;
import org.acme.domains.employee.dto.UpdateEmployeeRequest;
import org.acme.domains.manager.dto.CreateManagerRequest;
import org.acme.domains.manager.dto.ChangeManagerPasswordRequest;
import org.acme.domains.manager.dto.ManagerResponse;
import org.acme.domains.manager.dto.UpdateManagerEmailRequest;
import org.acme.domains.manager.dto.UpdateManagerRequest;
import org.acme.domains.onboarding.dto.OnboardingRequest;
import org.acme.domains.onboarding.dto.OnboardingResponse;
import org.acme.domains.partnership.dto.CreatePartnershipRequest;
import org.acme.domains.partnership.dto.PartnershipResponse;
import org.acme.domains.partnership.dto.UpdatePartnershipRequest;
import org.acme.domains.subscription.dto.CreateSubscriptionRequest;
import org.acme.domains.subscription.dto.SubscriptionResponse;
import org.acme.domains.redemption.dto.RedemptionPreviewResponse;
import org.acme.domains.redemption.dto.RedemptionResponse;
import org.acme.domains.redemption.dto.RedemptionTokenRequest;
import org.acme.domains.redemption.dto.RedemptionTokenResponse;
import org.acme.domains.sharedbenefit.dto.SharedBenefitResponse;

@RegisterForReflection(
        methods = true,
        fields = true,
        targets = {
                ErrorResponse.class,
                LoginRequest.class,
                LoginResponse.class,
                SwitchCompanyRequest.class,
                LoginContextData.class,
                CreateBenefitRequest.class,
                UpdateBenefitRequest.class,
                BenefitResponse.class,
                CategoryResponse.class,
                CreateBenefitAccessRequest.class,
                RejectBenefitAccessRequest.class,
                BenefitAccessRequestResponse.class,
                SharedBenefitResponse.class,
                RedemptionTokenRequest.class,
                RedemptionTokenResponse.class,
                RedemptionPreviewResponse.class,
                RedemptionResponse.class,
                CreateCompanyRequest.class,
                DeactivateCompanyRequest.class,
                UpdateCompanyRequest.class,
                CompanyResponse.class,
                CreateEmployeeRequest.class,
                UpdateEmployeeRequest.class,
                EmployeeResponse.class,
                CreateManagerRequest.class,
                ChangeManagerPasswordRequest.class,
                UpdateManagerEmailRequest.class,
                UpdateManagerRequest.class,
                ManagerResponse.class,
                OnboardingRequest.class,
                OnboardingRequest.ManagerRegistrationData.class,
                OnboardingResponse.class,
                CreatePartnershipRequest.class,
                UpdatePartnershipRequest.class,
                PartnershipResponse.class,
                CreateSubscriptionRequest.class,
                SubscriptionResponse.class
        }
)
public final class NativeJsonReflectionConfig {

    private NativeJsonReflectionConfig() {
    }
}
