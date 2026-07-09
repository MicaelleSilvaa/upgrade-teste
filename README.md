# Upgrade Teste — Liferay Workspace (DXP 2026.Q1 LTS)

Workspace Liferay com ambiente Docker Compose pronto para uso, módulos OSGi e theme de exemplo.

- **Banco de dados:** Hypersonic embutido (padrão do bundle, persistido no volume `liferay-data`)
- **Busca:** Elasticsearch *sidecar* embutido no próprio Liferay (padrão)
- **Imagem:** `liferay/dxp:2026.q1.10-lts` (**requer chave de ativação** — veja abaixo)

## Estrutura

| Caminho | Descrição |
|---------|-----------|
| `docker-compose.yml` | Container do Liferay DXP com hot deploy e overlay de configuração |
| `configs/docker/portal-ext.properties` | Properties do ambiente Docker (cadastro habilitado, wizard desligado, locale pt_BR) |
| `modules/portal-setup` | Cria as páginas do portal no site Guest no primeiro boot (idempotente) |
| `modules/welcome-web` | Portlet MVC "Bem-vindo" (saudação ao usuário logado) |
| `themes/upgrade-teste-theme` | Theme WAR (Theme Builder, parent `styled`) |

## Licença (obrigatória)

A imagem DXP quarterly não inclui trial. Coloque uma chave de ativação **7.4 developer**
(`activation-key-7.4-developer-*.xml`) em:

```
configs/docker/deploy/
```

Ela é copiada para `/opt/liferay/deploy` no boot do container. O diretório está no
`.gitignore` — a chave não é versionada.

## Como subir

```bash
# 1. Compila e copia os artefatos para bundles/osgi/* (montado no container)
blade gw deploy

# 2. Docker rootless: garante que o usuário `liferay` do container escreva nos mounts
chmod -R a+rwX bundles

# 3. Sobe o Liferay
docker compose up -d

# 4. Acompanha os logs até ver "Server startup in [X] ms"
docker compose logs -f liferay
```

Acesse **http://localhost:8080**.

- **Login admin:** `test@liferay.com` / senha `test` (sem troca forçada de senha — o
  módulo `portal-setup` remove a exigência no primeiro boot)
- **Cadastro:** link **"Criar conta"** na tela de login (auto cadastro habilitado)

## Páginas criadas automaticamente

O módulo `portal-setup` cria estas páginas públicas no site Guest no primeiro boot:

| Página | URL | Conteúdo |
|--------|-----|----------|
| Notícias | `/web/guest/noticias` | Blogs |
| Documentos | `/web/guest/documentos` | Documentos e Mídia |
| Painel | `/web/guest/painel` | Portlet Bem-vindo + Publicador de Conteúdo |

## Desenvolvimento

- **Hot deploy:** com o container rodando, `blade gw deploy` recompila e o container instala os artefatos automaticamente (volumes `bundles/osgi/*`).
- **Docker rootless:** arquivos criados pelo container dentro dos mounts (ex.: WAR do theme processado para `osgi/war`) ficam com dono do user namespace e o host não consegue lê-los. Se o `blade gw deploy` falhar com *"Cannot access a file in the destination directory"*, remova-os via container: `docker exec -u root upgrade-teste-liferay rm <arquivo>`.
- **Aplicar o theme:** após o deploy, em *Site Builder → Páginas → ⚙ → Aparência*, selecione **upgrade-teste-theme**.
- **Logs:** `docker compose logs -f liferay`
- **Debug remoto:** mude `LIFERAY_JPDA_ENABLED` para `"true"` no `docker-compose.yml` (porta 8000).
- **Licença própria:** coloque o `.xml` de ativação em `configs/docker/deploy/` (copiado para `/opt/liferay/deploy` no boot).

## Reset do ambiente

```bash
docker compose down -v   # remove o container E o banco (volume liferay-data)
```

No próximo `up`, o portal sobe zerado e o `portal-setup` recria as páginas.
