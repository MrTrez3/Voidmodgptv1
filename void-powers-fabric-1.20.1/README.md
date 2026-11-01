# Void Powers (Fabric)

Mod para Minecraft Java 1.20.1 + Fabric. Requer Java 17. As instruções para obter o JAR pelo GitHub estão abaixo.

## Gerar o JAR pelo GitHub Actions

1. Baixe e extraia `void-powers-fabric-1.20.1.zip`.
2. Crie uma conta no GitHub, instale o GitHub Desktop e entre na sua conta.
3. No GitHub Desktop, escolha **File → New repository**. Dê um nome, por exemplo `void-powers`, selecione uma pasta local, desmarque a opção de criar README inicial e crie o repositório.
4. Abra a pasta que você extraiu. Copie **todo o conteúdo de dentro** dela para a pasta do repositório criado. Copie também a pasta oculta `.github` (ela contém o workflow) e o arquivo oculto `.gitattributes`. Se não aparecerem no Explorador de Arquivos, ative **Exibir → Mostrar → Itens ocultos**.
5. Volte ao GitHub Desktop. Escreva uma mensagem como `Adicionar mod Void Powers`, clique **Commit to main** e depois **Publish repository**. Pode deixar privado ou público.
6. No GitHub, abra a aba **Actions** do repositório. A compilação `Build Void Powers` começa após o envio. Se aparecer um aviso para habilitar Actions, habilite e execute o workflow.
7. Quando a execução ficar verde, abra-a e baixe o artefato **void-powers-jar** no fim da página. Extraia o ZIP baixado; dentro estará `void-powers-1.0.0.jar`.
8. Para instalar, coloque esse `.jar` e o Fabric API compatível com Minecraft 1.20.1 na pasta `mods` do perfil Fabric.

Cada novo push ao repositório inicia outra compilação. O artefato fica disponível por 30 dias.

## Compilar no seu computador (opcional)

Com Java 17 instalado, abra um terminal nesta pasta e execute `gradlew.bat build` no Windows ou `./gradlew build` no macOS/Linux. O JAR aparecerá em `build/libs/void-powers-1.0.0.jar`. O Gradle Wrapper incluído baixa a versão necessária do Gradle automaticamente.

## Teclas padrão

| Poder | Tecla padrão |
|---|---|
| Transformar em Void | V |
| Apagar entidade mirada (até 24 blocos) | R |
| Segurar/soltar com telecinese | G |
| Arremessar entidade segurada | H |
| Ativar/desativar voo | F |
| Superforça (repulsão em área) | B |
| Rajada do Void | C |
| Teleporte até o bloco mirado | X |
| Alternar HUD | Sem tecla padrão; configure em Controles |

Reconfigure em Opções → Controles → Void Powers. Sem custo de XP, fome ou vida; a transformação não tem limite de tempo. O rastro fica até 3 minutos, com no máximo 32 rastros por cliente.

## Implementado

- Ações validadas no servidor, seleção por linha de visão e bloqueio por paredes.
- Apagamento em 30 ticks, com partículas e remoção da entidade; o rastro usa uma instância do modelo do tipo original escurecida no local.
- Voo, dano de queda suprimido durante a transformação, dano corpo a corpo aumentado, telecinese, arremesso, repulsão, rajada e teleporte.
- Efeitos de fumaça e HUD opcional.

A forma visual própria do jogador (modelo/skin sombreados) e os controles de intensidade, partículas e distância ainda não foram implementados. Este projeto-fonte ainda precisa ser compilado no GitHub Actions para confirmar o build e produzir o JAR.


