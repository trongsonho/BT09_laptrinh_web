package vn.iotstar.service;

import vn.iotstar.dto.UserDTO;

public interface UserService {
    UserDTO findById(Long id);
}
