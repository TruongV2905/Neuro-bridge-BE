package com.nqt.identity_service.mapper;

import com.nqt.identity_service.dto.request.register.ClientRegisterRequest;
import com.nqt.identity_service.dto.request.user.ClientRequest;
import com.nqt.identity_service.dto.request.user.ExpertRequest;
import com.nqt.identity_service.entity.ClientProfile;
import com.nqt.identity_service.entity.User;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ClientProfileMapper {

    @Mapping(target = "user", ignore = true)
    ClientProfile toEntity(ClientRegisterRequest request);


}
