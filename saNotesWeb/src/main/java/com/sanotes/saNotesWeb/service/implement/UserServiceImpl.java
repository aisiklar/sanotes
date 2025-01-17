package com.sanotes.saNotesWeb.service.implement;

import com.sanotes.saNotesPostgres.service.DAO.NoteBookRepository;
import com.sanotes.saNotesPostgres.service.DAO.RoleRepository;
import com.sanotes.saNotesPostgres.service.DAO.TagRepository;
import com.sanotes.saNotesPostgres.service.DAO.UserRepository;
import com.sanotes.saNotesPostgres.service.model.NoteBookModel;
import com.sanotes.saNotesPostgres.service.model.NotesModel;
import com.sanotes.saNotesPostgres.service.model.user.Role;
import com.sanotes.saNotesPostgres.service.model.user.RoleName;
import com.sanotes.saNotesPostgres.service.model.user.User;
import com.sanotes.saNotesWeb.exception.BadRequestException;
import com.sanotes.saNotesWeb.exception.ResourceNotFoundException;
import com.sanotes.saNotesWeb.exception.SANotesException;
import com.sanotes.saNotesWeb.exception.UnauthorizedException;
import com.sanotes.saNotesWeb.payload.*;
import com.sanotes.saNotesWeb.security.UserPrincipal;
import com.sanotes.saNotesWeb.service.UserService;
import com.sanotes.saNotesWeb.service.helper.NoteHelper;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private TagRepository tagRepository;
    @Autowired
    private NoteBookRepository noteBookRepository;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private NoteHelper noteHelper;

    @Override
    public BooleanResponse checkUsernameAvailability(String username) {
        Boolean isUserNameAvailable = !userRepository.existsByUsername(username);
        return new BooleanResponse(isUserNameAvailable);
    }

    @Override
    public BooleanResponse checkEmailAvailability(String email) {
        Boolean isEmailAvailable = !userRepository.existsByEmail(email);
        return new BooleanResponse(isEmailAvailable);
    }

    @Override
    public User addUser(User user) {
        if(userRepository.existsByEmail(user.getEmail())){
            ApiResponse apiResponse = new ApiResponse( Boolean.FALSE,"email is already in use");
            throw new BadRequestException(apiResponse);
        }
        if(userRepository.existsByUsername(user.getUsername())){
            ApiResponse apiResponse = new ApiResponse( Boolean.FALSE,"username is already in use");
            throw new BadRequestException(apiResponse);
        }

        List<Role> roles = new ArrayList<>();
        roles.add(
                roleRepository.findByName(RoleName.ROLE_USER)
                        .orElseThrow(()->new SANotesException("User roles could not set."))
        );
        user.setRoles(roles);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    @Override
    public User updateUser(User newUser, String username, UserPrincipal currentUser) {
        User user = userRepository.findByUsername(username).orElseThrow(
                ()->new ResourceNotFoundException("User","username",username));
        if(user.getId().equals(currentUser.getId()) ||
                currentUser.getAuthorities().contains(new SimpleGrantedAuthority(RoleName.ROLE_ADMIN.toString()))){
            user.setFirstname(newUser.getFirstname());
            user.setLastname(newUser.getLastname());
            user.setPassword(passwordEncoder.encode(newUser.getPassword()));
            return userRepository.save(user);
        }
        ApiResponse apiResponse = new ApiResponse(Boolean.FALSE,
                "You dont have permission to update user info of : "+ username);
        throw new UnauthorizedException(apiResponse);
    }

    @Override
    public ApiResponse deleteUser(String username, UserPrincipal currentUser) {
        User user = userRepository.findByUsername(username).orElseThrow(
                ()->new ResourceNotFoundException("User","username",username));
        if(!currentUser.getAuthorities().contains(new SimpleGrantedAuthority(RoleName.ROLE_ADMIN.toString()))){
            ApiResponse apiResponse = new ApiResponse(Boolean.FALSE,
                    "You dont have permission to delete user info of : "+ username);
            throw new UnauthorizedException(apiResponse);
        }

        userRepository.delete(user);
        return new ApiResponse(Boolean.TRUE,"You successfully delete user info of : "+ username);
    }

    @Override
    public ApiResponse giveAdmin(String username) {
        User user = userRepository.findByUsername(username).orElseThrow(
                ()->new ResourceNotFoundException("User","username",username));
        List<Role> roles = new ArrayList<>();
        roles.add(roleRepository.findByName(RoleName.ROLE_ADMIN)
                .orElseThrow(()->new SANotesException("User role cant set")));
        roles.add(roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(()->new SANotesException("User role cant set")));
        user.setRoles(roles);
        userRepository.save(user);
        return new ApiResponse(Boolean.TRUE,"You successfully give ADMIN role to user : "+ username);
    }

    @Override
    public ApiResponse removeAdmin(String username) {
        User user = userRepository.findByUsername(username).orElseThrow(
                ()->new ResourceNotFoundException("User","username",username));
        List<Role> roles = new ArrayList<>();
        roles.add(roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(()->new SANotesException("User role cant set")));
        user.setRoles(roles);
        userRepository.save(user);
        return new ApiResponse(Boolean.TRUE,"You successfully remove ADMIN role from user : "+ username);
    }

    @Override
    public UserResponse getUser(String username, UserPrincipal currentUser) {
        User user = userRepository.findByUsername(username).orElseThrow(
                ()->new ResourceNotFoundException("User","username",username));
        if(user.getId().equals(currentUser.getId())){
            List<NoteBookModel> noteBooks = user.getNoteBooks();
            for(int i = 0; i<noteBooks.size(); i++){
                NoteBookModel noteBook = noteBooks.get(i);
                List<NotesModel> notes=  noteBook.getNotes();
                notes = noteHelper.fillNotes(notes);
                noteBook.setNotes(notes);
                noteBooks.set(i,noteBook);
            }
            List<NoteBookResponse> noteBookResponses =
                    Arrays.asList(modelMapper.map(noteBooks, NoteBookResponse[].class));
            List<TagResponse> tagResponses =
                    Arrays.asList(modelMapper.map(user.getTags(), TagResponse[].class));
            return new UserResponse(user.getId(),user.getFirstname(),user.getLastname(),
                    user.getUsername(),user.getEmail(),noteBookResponses,tagResponses);
        }
        ApiResponse apiResponse = new ApiResponse(Boolean.FALSE,
                "You dont have permission get user info of : "+ username);
        throw new UnauthorizedException(apiResponse);

    }
}
