package domainmodels.user.unit;

import ch.course223.advanced.domainmodels.authority.Authority;
import ch.course223.advanced.domainmodels.authority.AuthorityDTO;
import ch.course223.advanced.domainmodels.role.Role;
import ch.course223.advanced.domainmodels.role.RoleDTO;
import ch.course223.advanced.domainmodels.user.User;
import ch.course223.advanced.domainmodels.user.UserDTO;
import ch.course223.advanced.domainmodels.user.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.*;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class UserController {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private UserService userService;

    @Before
    public void setUp(){

        //Business Objects (used in findById, findAll)
        Set<Authority> basicUserAuthorities = new HashSet<Authority>();
        basicUserAuthorities.add(new Authority().setName("USER_SEE_OWN"));
        basicUserAuthorities.add(new Authority().setName("USER_MODIFY_OWN"));

        Set<Authority> adminUserAuthorities = new HashSet<Authority>();
        adminUserAuthorities.add(new Authority().setName("USER_SEE_OWN"));
        adminUserAuthorities.add(new Authority().setName("USER_SEE_GLOBAL"));
        adminUserAuthorities.add(new Authority().setName("USER_CREATE"));
        adminUserAuthorities.add(new Authority().setName("USER_MODIFY_OWN"));
        adminUserAuthorities.add(new Authority().setName("USER_MODIFY_GLOBAL"));
        adminUserAuthorities.add(new Authority().setName("USER_DELETE"));

        Set<Role> basicUserRoles = new HashSet<Role>();
        basicUserRoles.add(new Role().setName("BASIC_USER").setAuthorities(basicUserAuthorities);

        Set<Role> adminUserRoles = new HashSet<Role>();
        adminUserRoles.add(new Role().setName("ADMIN_USER").setAuthorities(adminUserAuthorities));

        User basicUser = new User().setRoles(basicUserRoles).setFirstName("jane").setLastName("doe").setEmail("jane.doe@noseryoung.ch");
        User adminUser = new User().setRoles(adminUserRoles).setFirstName("john").setLastName("doe").setEmail("john.doe@noseryoung.ch");

        //Mocks
        given(userService.findById(anyString())).willReturn(basicUser);

        given(userService.findAll()).willReturn(Arrays.asList(basicUser, adminUser));

        given(userService.save(any(User.class))).will(invocation -> {
            UUID uuid = UUID.randomUUID();
            User userDTO = invocation.getArgument(0);
            return userDTO.setId(uuid.toString());
        });

        given(userService.updateById(anyString(), any(User.class))).will(invocation -> {
            if ("non-existent".equals(invocation.getArgument(0))) throw new NoSuchElementException();

            return ((User) invocation.getArgument(1)).setId(invocation.getArgument(0));
        });

    }

    @Test
    @WithMockUser(roles = {"BASIC_USER"})
    public void findById_requestUserById_returnsUser() throws Exception {
        UUID uuid = UUID.randomUUID();
        mvc.perform(
                MockMvcRequestBuilders.get("/users/{id}", uuid.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.firstName").value("john"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.lastName").value("doe"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.email").value("john.doe@noseryoung.ch"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.roles[0].name").value("USER"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.roles[0].[0].name").value("USER_SEE_OWN"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.roles[0].[1].name").value("USER_MODIFY_OWN"));

        verify(userService, times(1)).findById(uuid.toString());
    }

    @Test
    @WithMockUser
    public void findAll_requestAllUsers_returnsAllUsers() throws Exception {
        mvc.perform(
                MockMvcRequestBuilders.get("/users")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$", hasSize(2)))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].email").value("john.doe@noseryoung.ch"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].email").value("jane.doe@noseryoung.ch"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].firstName").value("john"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].firstName").value("jane"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].lastName").value("doe"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].lastName").value("doe"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].roles[0].name").value("ADMIN"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].roles[0].name").value("USER"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].roles[0].[0].name").value("USER_SEE_OWN"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].roles[0].[1].name").value("USER_MODIFY_OWN"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].roles[0].[0].name").value("USER_SEE_OWN"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].roles[0].[1].name").value("USER_MODIFY_OWN"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].roles[0].[2].name").value("USER_CREATE"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].roles[0].[3].name").value("USER_MODIFY_OWN"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].roles[0].[4].name").value("USER_MODIFY_GLOBAL"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].roles[0].[5].name").value("USER_DELETE"));

        verify(userService, times(1)).findAll();
    }

    @Test
    @WithMockUser
    public void create_deliverUserDTOToCreate_thenReturnCreatedUserDTO() throws Exception {
        Set<AuthorityDTO> basicUserAuthorityDTOS = new HashSet<AuthorityDTO>();
        basicUserAuthorityDTOS.add(new AuthorityDTO().setName("USER_SEE_OWN"));
        basicUserAuthorityDTOS.add(new AuthorityDTO().setName("USER_MODIFY_OWN"));

        Set<RoleDTO> basicUserRoleDTOS = new HashSet<RoleDTO>();
        basicUserRoleDTOS.add(new RoleDTO().setName("USER").setAuthorities(basicUserAuthorityDTOS);

        UserDTO userDTO = new UserDTO().setRoles(basicUserRoleDTOS).setFirstName("jane").setLastName("doe").setEmail("jane.doe@noseryoung.ch");

        String userDTOAsJsonString = new ObjectMapper().writeValueAsString(userDTO);

        mvc.perform(
                MockMvcRequestBuilders
                        .post("/users")
                        .content(userDTOAsJsonString)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.firstName").value("john"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.lastName").value("doe"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.email").value("john.doe@noseryoung.ch"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.roles[0].name").value("USER"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.roles[0].[0].name").value("USER_SEE_OWN"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.roles[0].[1].name").value("USER_MODIFY_OWN"));

        verify(userService, times(1)).save(any(User.class));
    }

    @Test
    @WithMockUser
    public void updateUserById_deliverUserDTOToUpdate_thenReturnUpdatedUserDTO() throws Exception {
        UUID uuid = UUID.randomUUID();
        Set<AuthorityDTO> basicUserAuthorityDTOS = new HashSet<AuthorityDTO>();
        basicUserAuthorityDTOS.add(new AuthorityDTO().setName("USER_SEE_OWN"));
        basicUserAuthorityDTOS.add(new AuthorityDTO().setName("USER_MODIFY_OWN"));

        Set<RoleDTO> basicUserRoleDTOS = new HashSet<RoleDTO>();
        basicUserRoleDTOS.add(new RoleDTO().setName("USER").setAuthorities(basicUserAuthorityDTOS);

        UserDTO userDTO = new UserDTO().setRoles(basicUserRoleDTOS).setFirstName("jane").setLastName("doe").setEmail("jane.doe@noseryoung.ch");

        String userDTOAsJsonString = new ObjectMapper().writeValueAsString(userDTO);

        mvc.perform(
                MockMvcRequestBuilders.put("/users/{id}", uuid.toString())
                        .content(userDTOAsJsonString)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.firstName").value("john"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.lastName").value("doe"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.email").value("john.doe@noseryoung.ch"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.roles[0].name").value("USER"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.roles[0].[0].name").value("USER_SEE_OWN"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.roles[0].[1].name").value("USER_MODIFY_OWN"));

        verify(userService, times(1)).updateById(anyString(), any(User.class));
    }

}