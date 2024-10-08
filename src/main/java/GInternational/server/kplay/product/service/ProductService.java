package GInternational.server.kplay.product.service;

import GInternational.server.kplay.product.dto.ProductRequestDTO;
import GInternational.server.kplay.product.dto.ProductResponseDTO;
import GInternational.server.kplay.product.entity.MegaProduct;
import GInternational.server.kplay.product.entity.Product;
import GInternational.server.kplay.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static GInternational.server.kplay.product.dto.ProductResponseDTO.*;

@Service
@Transactional(value = "clientServerTransactionManager")
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;


    /**
     * 제품 정보 목록을 데이터베이스에 저장하고 처리 결과를 반환.
     *
     * @param data 제품 정보를 담은 DTO 목록
     * @return ProductResponseDTO 처리 상태를 나타내는 DTO
     */
    public ProductResponseDTO insertProducts(List<ProductRequestDTO> data) {
        List<ProductData> productList = new ArrayList<>();

        for (ProductRequestDTO list : data) {
            Product product = new Product();
            product.setPrd_id(list.getPrd_id());
            product.setPrd_name(list.getPrd_name());
            productRepository.save(product);
        }
        ProductResponseDTO response = new ProductResponseDTO();
        response.setStatus(1);
        return response;
    }

    /**
     * 지정된 인덱스 범위 내의 제품 정보를 조회.
     *
     * @param startIndex 시작 인덱스
     * @param endIndex 종료 인덱스
     * @return List<Product> 조회된 제품 정보 목록
     */
    public List<Product> searchByPrd(int startIndex,int endIndex) {
        return productRepository.searchByProvider(startIndex,endIndex);
    }
}
