# Lazy Replication

## Frontend

Na arquitetura *Gossip*, um cliente comunica com uma réplica através do seu *frontend*, por meio de duas operações: *query* (read) e *update* (write).

Este inclui em todos os seus pedidos a uma réplica uma operação e o seu *timestamp*, uma entrada por réplica que representa a última atualização vista pelo cliente nessa mesma.
Para cada resposta da réplica, o *frontend* funde o seu timestamp com o que veio anexado na resposta.

---

## Replica Manager

A cada réplica está associado um *Replica Manager*, que, para além do estado da Turma, mantém um *UpdateLog* (coleção com registos - *LogRecords* daqui em diante - que contêm updates requisitados por *frontends* ou comunicados por outras réplicas por *Gossip*. Ao estado e ao *UpdateLog* estão associados *timestamps*, mantendo-se também uma tabela de *timestamps* para cada uma das outras réplicas, por forma a estimar (de forma pessimista) quando é que cada réplica tomou conhecimento de um dado *UpdateLog*, por forma a poder esvaziar o *UpdateLog* periodicamente.

---

## Garantias de Sessão

Esta solução oferece a garantia de *Writes Follow Reads*. Desta forma, uma operação só é executada quando o *timestamp* de *writes* do cliente é menor ou igual ao *timestamp* do estado; em caso contrário, coloca-se o *LogRecord* no *log* à espera de *LogRecords* de outras réplicas até que a condição de execução referida se verifique.

---

## Conflict Solving

Fundindo um *LogRecord* com o *UpdateLog* existente, conflitos poderão surgir.

### Deteção:
Dois *LogRecords* entram em conflito se e só se foram produzidos de forma concorrente em réplicas diferentes (i.e., entre propagações de estado entre elas). De acordo com a lógica do problema, resumem-se a dois casos:
    
1 - Enroll:
Dois estudantes matriculam-se na turma com 1 vaga em réplicas diferentes;

2 - CloseEnrollments:
Um professor fecha as inscrições de matrícula numa réplica, mas noutra mantêm-se abertas e recebem matrículas de novos estudantes entretanto;

### Resolução:
A nossa solução usa um relógio físico do momento da receção do *update* no *ReplicaManager* como critério de desempate, sendo estampado nas instruções de *update* mandadas pelo *frontend*. \
Periodicamente, verificam-se as entradas do *Update Log* por ordem de *happened-before*, utilizando os relógios físicos para o desempate da igualdade de relógios. Para cada *LogRecord* cuja operação ainda não foi executada ou está *suspensa* (ver mais à frente), verificam-se *LogRecords* com relógio físico superior e com operações conflituosas (e.g., a última inscrição efetuada com sucesso, no caso (1), e todas as inscrições procedentes efetuadas com sucesso, no caso (2)). Estes *LogRecords* têm os respetivos efeitos revertidos e ficam marcados como *suspensos*. Quando um *LogRecord* *suspenso* é executado com sucesso (i.e., sem provocar qualquer exceção de domínio), deixa de estar suspenso.
