-- spec-20 (#23): corretora que saiu do mercado; separado de ativo, que segue significando "não excluída"
ALTER TABLE corretora ADD COLUMN descontinuada BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE corretora ADD COLUMN descontinuada_em TIMESTAMP;
