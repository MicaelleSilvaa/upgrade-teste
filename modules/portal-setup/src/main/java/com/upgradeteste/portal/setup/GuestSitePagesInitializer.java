package com.upgradeteste.portal.setup;

import com.liferay.layout.page.template.kernel.provider.LayoutPageTemplateEntryLayoutProvider;
import com.liferay.portal.instance.lifecycle.BasePortalInstanceLifecycleListener;
import com.liferay.portal.instance.lifecycle.PortalInstanceLifecycleListener;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.GroupConstants;
import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.model.LayoutConstants;
import com.liferay.portal.kernel.model.LayoutTypePortlet;
import com.liferay.portal.kernel.model.PasswordPolicy;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.portlet.PortletIdCodec;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.service.LayoutLocalService;
import com.liferay.portal.kernel.service.PasswordPolicyLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.util.HashMapBuilder;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Cria as paginas iniciais do site Guest no primeiro boot e remove a troca
 * obrigatoria de senha do primeiro login. Roda de forma idempotente: paginas
 * ja existentes (mesma friendly URL) nao sao recriadas.
 */
@Component(service = PortalInstanceLifecycleListener.class)
public class GuestSitePagesInitializer
	extends BasePortalInstanceLifecycleListener {

	@Override
	public void portalInstanceRegistered(Company company) throws Exception {
		long companyId = company.getCompanyId();

		if (_log.isInfoEnabled()) {
			_log.info("Executando setup do portal para company " + companyId);
		}

		_disablePasswordChangeRequired(companyId);

		Group group = _groupLocalService.getGroup(
			companyId, GroupConstants.GUEST);

		User guestUser = _userLocalService.getGuestUser(companyId);

		_addPage(
			guestUser, group, "Notícias", "/noticias",
			new String[] {_PORTLET_ID_BLOGS});
		_addPage(
			guestUser, group, "Documentos", "/documentos",
			new String[] {_PORTLET_ID_DOCUMENT_LIBRARY});
		_addPage(
			guestUser, group, "Painel", "/painel",
			new String[] {_PORTLET_ID_WELCOME, _PORTLET_ID_ASSET_PUBLISHER});
		_addPage(
			guestUser, group, "Fórum", "/forum",
			new String[] {_PORTLET_ID_MESSAGE_BOARDS});
		_addPage(
			guestUser, group, "Upgrades", "/upgrades",
			new String[] {_PORTLET_ID_WIKI});
		_addPage(
			guestUser, group, "Eh-babado", "/testinho",
			new String[] {_PORTLET_ID_WIKI});
	}

	private void _addPage(
			User user, Group group, String name, String friendlyURL,
			String[] portletIds)
		throws Exception {

		Layout layout = _layoutLocalService.fetchLayoutByFriendlyURL(
			group.getGroupId(), false, friendlyURL);

		Locale locale = user.getLocale();

		if (layout == null) {
			Map<Locale, String> nameMap = HashMapBuilder.put(
				locale, name
			).build();

			ServiceContext serviceContext = new ServiceContext();

			serviceContext.setAddGroupPermissions(true);
			serviceContext.setAddGuestPermissions(true);

			layout = _layoutLocalService.addLayout(
				null, user.getUserId(), group.getGroupId(), false,
				LayoutConstants.DEFAULT_PARENT_LAYOUT_ID, nameMap, nameMap,
				null, null, null, LayoutConstants.TYPE_PORTLET, null, false,
				false,
				HashMapBuilder.put(
					locale, friendlyURL
				).build(),
				serviceContext);

			if (_log.isInfoEnabled()) {
				_log.info("Página criada: " + name + " (" + friendlyURL + ")");
			}
		}

		LayoutTypePortlet layoutTypePortlet =
			(LayoutTypePortlet)layout.getLayoutType();

		layoutTypePortlet.setLayoutTemplateId(user.getUserId(), "1_column");

		boolean modified = false;

		Set<String> existingRootPortletIds = new HashSet<>();

		for (String existingPortletId : layoutTypePortlet.getPortletIds()) {
			existingRootPortletIds.add(
				PortletIdCodec.decodePortletName(existingPortletId));
		}

		for (String portletId : portletIds) {
			if (!existingRootPortletIds.contains(portletId)) {
				String addedPortletId = layoutTypePortlet.addPortletId(
					user.getUserId(), portletId, "column-1", -1, false);

				if (_log.isInfoEnabled()) {
					_log.info(
						"Portlet " + portletId + " em " + friendlyURL + ": " +
							addedPortletId);
				}

				modified = true;
			}
		}

		if (modified) {
			_layoutLocalService.updateLayout(layout);
		}
	}

	private void _disablePasswordChangeRequired(long companyId)
		throws Exception {

		PasswordPolicy passwordPolicy =
			_passwordPolicyLocalService.getDefaultPasswordPolicy(companyId);

		if ((passwordPolicy != null) && passwordPolicy.isChangeRequired()) {
			passwordPolicy.setChangeRequired(false);

			_passwordPolicyLocalService.updatePasswordPolicy(passwordPolicy);
		}

		User adminUser = _userLocalService.fetchUserByEmailAddress(
			companyId, "test@liferay.com");

		if ((adminUser != null) && adminUser.isPasswordReset()) {
			adminUser.setPasswordReset(false);

			_userLocalService.updateUser(adminUser);

			if (_log.isInfoEnabled()) {
				_log.info(
					"Troca de senha obrigatória removida do usuário admin");
			}
		}
	}

	private static final String _PORTLET_ID_ASSET_PUBLISHER =
		"com_liferay_asset_publisher_web_portlet_AssetPublisherPortlet";

	private static final String _PORTLET_ID_BLOGS =
		"com_liferay_blogs_web_portlet_BlogsPortlet";

	private static final String _PORTLET_ID_DOCUMENT_LIBRARY =
		"com_liferay_document_library_web_portlet_DLPortlet";

	private static final String _PORTLET_ID_MESSAGE_BOARDS =
		"com_liferay_message_boards_web_portlet_MBPortlet";

	private static final String _PORTLET_ID_WELCOME =
		"com_upgradeteste_welcome_WelcomePortlet";

	private static final String _PORTLET_ID_WIKI =
		"com_liferay_wiki_web_portlet_WikiPortlet";

	private static final Log _log = LogFactoryUtil.getLog(
		GuestSitePagesInitializer.class);

	@Reference
	private GroupLocalService _groupLocalService;

	@Reference
	private LayoutLocalService _layoutLocalService;

	@Reference
	private LayoutPageTemplateEntryLayoutProvider
		_layoutPageTemplateEntryLayoutProvider;

	@Reference
	private PasswordPolicyLocalService _passwordPolicyLocalService;

	@Reference
	private UserLocalService _userLocalService;

	@Reference(
		target = "(jakarta.portlet.name=com_upgradeteste_welcome_WelcomePortlet)"
	)
	private jakarta.portlet.Portlet _welcomePortlet;

}
