package com.bnfix.ubm.shared.nativeimage;

import java.util.List;

import com.bnfix.ubm.api.ApiError;
import com.bnfix.ubm.domains.account.Account;
import com.bnfix.ubm.domains.announcement.Announcement;
import com.bnfix.ubm.domains.announcement.AnnouncementRecipient;
import com.bnfix.ubm.domains.announcement.dto.CreateAnnouncementRequest;
import com.bnfix.ubm.domains.announcement.dto.EmployeeAnnouncement;
import com.bnfix.ubm.domains.announcement.dto.EmployeeAnnouncementPage;
import com.bnfix.ubm.domains.announcement.dto.ManagerAnnouncement;
import com.bnfix.ubm.domains.announcement.dto.ManagerAnnouncementPage;
import com.bnfix.ubm.domains.announcement.dto.UnreadCountResponse;
import com.bnfix.ubm.domains.auth.dto.LoginContextData;
import com.bnfix.ubm.domains.auth.dto.LoginRequest;
import com.bnfix.ubm.domains.auth.dto.LoginResponse;
import com.bnfix.ubm.domains.auth.dto.SwitchCompanyRequest;
import com.bnfix.ubm.domains.benefit.Benefit;
import com.bnfix.ubm.domains.benefit.dto.BenefitResponse;
import com.bnfix.ubm.domains.benefit.dto.CreateBenefitRequest;
import com.bnfix.ubm.domains.benefit.dto.UpdateBenefitRequest;
import com.bnfix.ubm.domains.benefitrequest.BenefitAccessRequest;
import com.bnfix.ubm.domains.benefitrequest.BenefitAccessRequestStatus;
import com.bnfix.ubm.domains.benefitrequest.dto.BenefitAccessRequestResponse;
import com.bnfix.ubm.domains.benefitrequest.dto.CreateBenefitAccessRequest;
import com.bnfix.ubm.domains.benefitrequest.dto.RejectBenefitAccessRequest;
import com.bnfix.ubm.domains.category.Category;
import com.bnfix.ubm.domains.category.dto.CategoryResponse;
import com.bnfix.ubm.domains.company.Company;
import com.bnfix.ubm.domains.company.dto.CompanyResponse;
import com.bnfix.ubm.domains.company.dto.CreateCompanyRequest;
import com.bnfix.ubm.domains.company.dto.DeactivateCompanyRequest;
import com.bnfix.ubm.domains.company.dto.UpdateCompanyRequest;
import com.bnfix.ubm.domains.employee.Employee;
import com.bnfix.ubm.domains.employee.EmployeeStatus;
import com.bnfix.ubm.domains.employee.dto.CreateEmployeeRequest;
import com.bnfix.ubm.domains.employee.dto.EmployeeResponse;
import com.bnfix.ubm.domains.employee.dto.UpdateEmployeeRequest;
import com.bnfix.ubm.domains.manager.Manager;
import com.bnfix.ubm.domains.manager.dto.ChangeManagerPasswordRequest;
import com.bnfix.ubm.domains.manager.dto.CreateManagerRequest;
import com.bnfix.ubm.domains.manager.dto.ManagerResponse;
import com.bnfix.ubm.domains.manager.dto.UpdateManagerEmailRequest;
import com.bnfix.ubm.domains.onboarding.dto.OnboardingRequest;
import com.bnfix.ubm.domains.onboarding.dto.OnboardingResponse;
import com.bnfix.ubm.domains.partnership.Partnership;
import com.bnfix.ubm.domains.partnership.PartnershipStatus;
import com.bnfix.ubm.domains.partnership.dto.CreatePartnershipRequest;
import com.bnfix.ubm.domains.partnership.dto.PartnershipResponse;
import com.bnfix.ubm.domains.redemption.BenefitRedemption;
import com.bnfix.ubm.domains.redemption.RedemptionToken;
import com.bnfix.ubm.domains.redemption.RedemptionTokenStatus;
import com.bnfix.ubm.domains.redemption.dto.RedemptionPreviewResponse;
import com.bnfix.ubm.domains.redemption.dto.RedemptionResponse;
import com.bnfix.ubm.domains.redemption.dto.RedemptionTokenRequest;
import com.bnfix.ubm.domains.redemption.dto.RedemptionTokenResponse;
import com.bnfix.ubm.domains.shared.domain.CNPJ;
import com.bnfix.ubm.domains.shared.domain.CPF;
import com.bnfix.ubm.domains.shared.enums.Role;
import com.bnfix.ubm.domains.sharedbenefit.dto.SharedBenefitResponse;
import com.bnfix.ubm.domains.subscription.Subscription;
import com.bnfix.ubm.domains.subscription.dto.CreateSubscriptionRequest;
import com.bnfix.ubm.domains.subscription.dto.SubscriptionResponse;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;

public final class NativeRuntimeHints implements RuntimeHintsRegistrar {

	private static final MemberCategory[] REFLECTION = MemberCategory.values();

	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
		registerArrayTypes(hints);
		registerDomainTypes(hints);
	}

	private void registerArrayTypes(RuntimeHints hints) {
		hints.reflection().registerType(TypeReference.of("java.util.UUID[]"), REFLECTION);
		hints.reflection().registerType(TypeReference.of("java.lang.Long[]"), REFLECTION);
	}

	private void registerDomainTypes(RuntimeHints hints) {
		List<TypeReference> types = TypeReference.listOf(
				Account.class, Announcement.class, AnnouncementRecipient.class,
				Benefit.class, BenefitAccessRequest.class, BenefitRedemption.class,
				Category.class, Company.class, Employee.class, Manager.class,
				Partnership.class, RedemptionToken.class, Subscription.class,
				CPF.class, CNPJ.class,
				BenefitAccessRequestStatus.class, EmployeeStatus.class,
				PartnershipStatus.class, RedemptionTokenStatus.class, Role.class,
				ApiError.class,
				CreateAnnouncementRequest.class, EmployeeAnnouncement.class,
				EmployeeAnnouncementPage.class, ManagerAnnouncement.class,
				ManagerAnnouncementPage.class, UnreadCountResponse.class,
				LoginContextData.class, LoginRequest.class, LoginResponse.class,
				SwitchCompanyRequest.class,
				BenefitResponse.class, CreateBenefitRequest.class, UpdateBenefitRequest.class,
				BenefitAccessRequestResponse.class, CreateBenefitAccessRequest.class,
				RejectBenefitAccessRequest.class,
				CategoryResponse.class,
				CompanyResponse.class, CreateCompanyRequest.class,
				DeactivateCompanyRequest.class, UpdateCompanyRequest.class,
				CreateEmployeeRequest.class, EmployeeResponse.class, UpdateEmployeeRequest.class,
				ChangeManagerPasswordRequest.class, CreateManagerRequest.class,
				ManagerResponse.class, UpdateManagerEmailRequest.class,
				OnboardingRequest.class, OnboardingRequest.ManagerRegistrationData.class,
				OnboardingResponse.class,
				CreatePartnershipRequest.class, PartnershipResponse.class,
				RedemptionPreviewResponse.class, RedemptionResponse.class,
				RedemptionTokenRequest.class, RedemptionTokenResponse.class,
				SharedBenefitResponse.class,
				CreateSubscriptionRequest.class, SubscriptionResponse.class
		);
		for (TypeReference type : types) {
			hints.reflection().registerType(type, REFLECTION);
		}
	}
}
