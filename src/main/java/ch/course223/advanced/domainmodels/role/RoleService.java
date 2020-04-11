package ch.course223.advanced.domainmodels.role;

import ch.nyp.noa.config.generic.ExtendedService;

import java.util.NoSuchElementException;

public interface RoleService extends ExtendedService<Role> {

	Role findByName(String name);

	void deleteByName(String name);

	Role addAuthorityToRole(String roleId, String authorityId) throws NoSuchElementException;

	Role removeAuthorityFromRole(String roleId, String authorityId) throws NoSuchElementException;
}