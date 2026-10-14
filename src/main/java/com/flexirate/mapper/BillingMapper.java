package com.flexirate.mapper;

import com.flexirate.domain.*;
import com.flexirate.dto.*;
import com.flexirate.util.BillingDates;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * MapStruct generates all field/collection copying; date conversions delegate to BillingDates.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR, uses = BillingDates.class)
public interface BillingMapper {
    /**
     * Maps the full bill list and header-only invalid count into an immutable response envelope.
     */
    @Mapping(target = "users", source = "bills")
    BillingResponseDto toResponse(List<Bill> bills, int invalidEventCount);

    /**
     * Bulk-maps API users, resolving their timezone strings into ZoneId values.
     */
    List<User> toUsers(List<UserDto> userDtos);

    /**
     * Bulk-maps domain bills to API bills, including nested tier/surcharge collections.
     */
    List<UserBillDto> toUserBills(List<Bill> bills);

    /**
     * Maps one user; the generated collection mapper invokes this method for each input item.
     */
    User toUser(UserDto userDto);

    /**
     * Parses a DTO timestamp into a UTC instant; ValidationService then enforces chunk alignment.
     */
    UsageChunk toChunk(EventDto eventDto);

    /**
     * Maps one domain pricing result without recalculating or rounding monetary values.
     */
    UserBillDto toDto(Bill bill);

    /**
     * Copies the selected tier's hours/rate/base component into its API representation.
     */
    TierDto toDto(Tier tierBreakdown);

    /**
     * Copies a paid UTC peak surcharge into its API representation.
     */
    SurchargeDto toDto(Surcharge appliedSurcharge);
}
