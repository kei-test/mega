package GInternational.server.api.mapper;


import GInternational.server.api.dto.WalletResponseDTO;

import GInternational.server.common.generic.GenericMapper;
import GInternational.server.api.entity.Wallet;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface WalletResponseMapper extends GenericMapper<WalletResponseDTO, Wallet> {
    WalletResponseMapper INSTANCE = Mappers.getMapper(WalletResponseMapper.class);
}
