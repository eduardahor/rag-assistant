# API da Loja Fictícia

## Autenticação
A API usa bearer tokens. Envie o token no header Authorization,
no formato "Bearer SEU_TOKEN". Tokens expiram em 1 hora.

## Limites
Cada cliente pode fazer até 100 requisições por minuto.
Ao exceder o limite, a API retorna o status HTTP 429.

## Pedidos
Para criar um pedido, envie POST /orders com os campos produto e quantidade.
Para consultar um pedido, use GET /orders/{id}.