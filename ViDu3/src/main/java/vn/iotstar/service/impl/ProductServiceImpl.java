package vn.iotstar.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.iotstar.dto.ProductDTO;
import vn.iotstar.entity.Product;
import vn.iotstar.entity.User;
import vn.iotstar.mapper.ProductMapper;
import vn.iotstar.repository.ProductRepository;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.service.CloudinaryService;
import vn.iotstar.service.CloudinaryUploadResult;
import vn.iotstar.service.ProductService;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ProductMapper mapper;
    private final CloudinaryService cloudinaryService;

    @Override
    @Transactional(readOnly = true)
    public Page<ProductDTO> findAll(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0), Math.max(size, 1),
                Sort.by(Sort.Direction.DESC, "id")
        );
        return productRepository.search(keyword == null ? "" : keyword, pageable)
                .map(mapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDTO findById(Long id) {
        return mapper.toDTO(productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product không tồn tại: " + id)));
    }

    @Override
    @Transactional
    public ProductDTO create(ProductDTO dto, MultipartFile image) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User không tồn tại: " + dto.getUserId()));
        Product product = mapper.toEntity(dto);
        product.setUser(user);

        if (image != null && !image.isEmpty()) {
            CloudinaryUploadResult r = cloudinaryService.upload(image);
            if (r != null && r.url() != null) {
                product.setImageUrl(r.url() + "|" + (r.publicId() != null ? r.publicId() : ""));
            }
        }
        return mapper.toDTO(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductDTO update(Long id, ProductDTO dto, MultipartFile image) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product không tồn tại: " + id));

        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());

        if (image != null && !image.isEmpty()) {
            String old = product.getImageUrl();
            if (old != null && old.contains("|")) {
                String oldPublicId = old.substring(old.indexOf('|') + 1);
                if (!oldPublicId.isBlank()) {
                    cloudinaryService.delete(oldPublicId);
                }
            }
            CloudinaryUploadResult r = cloudinaryService.upload(image);
            if (r != null && r.url() != null) {
                product.setImageUrl(r.url() + "|" + (r.publicId() != null ? r.publicId() : ""));
            }
        }
        return mapper.toDTO(productRepository.save(product));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product không tồn tại: " + id));
        String image = product.getImageUrl();
        if (image != null && image.contains("|")) {
            String publicId = image.substring(image.indexOf('|') + 1);
            if (!publicId.isBlank()) {
                cloudinaryService.delete(publicId);
            }
        }
        productRepository.delete(product);
    }

    @Override
    @Transactional(readOnly = true)
    public long countProducts() {
        return productRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public long countByUser(Long userId) {
        return productRepository.countByUserId(userId);
    }
}
