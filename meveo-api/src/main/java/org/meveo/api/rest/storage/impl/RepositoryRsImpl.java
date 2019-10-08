package org.meveo.api.rest.storage.impl;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.interceptor.Interceptors;

import org.meveo.api.BaseCrudApi;
import org.meveo.api.dto.ActionStatus;
import org.meveo.api.dto.ActionStatusEnum;
import org.meveo.api.dto.response.storage.RepositoriesResponseDto;
import org.meveo.api.dto.response.storage.RepositoryResponseDto;
import org.meveo.api.logging.WsRestApiInterceptor;
import org.meveo.api.rest.impl.BaseCrudRs;
import org.meveo.api.rest.storage.RepositoryRs;
import org.meveo.api.storage.RepositoryApi;
import org.meveo.api.storage.RepositoryDto;
import org.meveo.model.storage.Repository;

/**
 * @author Edward P. Legaspi | czetsuya@gmail.com
 * @lastModifiedVersion 6.4.0
 */
@RequestScoped
@Interceptors({ WsRestApiInterceptor.class })
public class RepositoryRsImpl extends BaseCrudRs<Repository, RepositoryDto> implements RepositoryRs {

	@Inject
	private RepositoryApi repositoryApi;

	@Override
	public ActionStatus create(RepositoryDto postData) {
		ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

		try {
			repositoryApi.create(postData);

		} catch (Exception e) {
			processException(e, result);
		}
		return result;
	}

	@Override
	public ActionStatus update(RepositoryDto postData) {
		ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

		try {
			repositoryApi.update(postData);

		} catch (Exception e) {
			processException(e, result);
		}
		return result;
	}

	@Override
	public ActionStatus createOrUpdate(RepositoryDto postData) {
		ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

		try {
			repositoryApi.createOrUpdate(postData);

		} catch (Exception e) {
			processException(e, result);
		}
		return result;
	}

	@Override
	public RepositoryResponseDto find(String code) {
		RepositoryResponseDto result = new RepositoryResponseDto();
		try {
			result.setRepository(repositoryApi.find(code));

		} catch (Exception e) {
			processException(e, result.getActionStatus());
		}

		return result;
	}

	@Override
	public RepositoriesResponseDto list() {
		RepositoriesResponseDto result = new RepositoriesResponseDto();
		try {
			result.setRepositories(repositoryApi.findAll());

		} catch (Exception e) {
			processException(e, result.getActionStatus());
		}

		return result;
	}

	@Override
	public ActionStatus remove(String code) {
		ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

		try {
			repositoryApi.remove(code);

		} catch (Exception e) {
			processException(e, result);
		}
		return result;
	}

	@Override
	public BaseCrudApi<Repository, RepositoryDto> getBaseCrudApi() {
		return repositoryApi;
	}

	@Override
	public ActionStatus removeHierarchy(String code) {
		ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

		try {
			repositoryApi.removeHierarchy(code);

		} catch (Exception e) {
			processException(e, result);
		}
		return result;
	}

}
