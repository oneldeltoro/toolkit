-- -------------------------------------------------------------
-- Table structure for Transformation service
-- -------------------------------------------------------------

-- -------------------------------------------------------------
-- Table structure for bibliographic_manifestation
-- -------------------------------------------------------------

-- ben's custom tables --
create table bibsProcessedLongId (
	org_code            VARCHAR(16)     not null,
	bib_001             BIGINT       not null,
	record_id           BIGINT       not null,
	primary KEY (org_code, bib_001, record_id),
	KEY bibsProcessedLongId_bib_001(bib_001),
	KEY bibsProcessedLongId_record_id(record_id)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

create table bibsProcessedStringId (
	org_code            VARCHAR(16)     not null,
	bib_001             VARCHAR(64)     not null,
	record_id           BIGINT       not null,
	primary KEY (org_code, bib_001, record_id),
	KEY bibsProcessedStringId_bib_001(bib_001),
	KEY bibsProcessedStringId_record_id(record_id)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

create table bibsYet2ArriveLongId (
	org_code            VARCHAR(16)     not null,
	bib_001             BIGINT       not null,
	record_id           BIGINT       not null,
	primary KEY (org_code, bib_001, record_id),
	KEY bibsYet2ArriveLongId_bib_001(bib_001),
	KEY bibsYet2ArriveLongId_record_id(record_id)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

create table bibsYet2ArriveStringId (
	org_code            VARCHAR(16)     not null,
	bib_001             VARCHAR(64)     not null,
	record_id           BIGINT       not null,
	primary KEY (org_code, bib_001, record_id),
	KEY bibsYet2ArriveStringId_bib_001(bib_001),
	KEY bibsYet2ArriveStringId_record_id(record_id)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

create table held_holdings (
	org_code            VARCHAR(16)     not null,
	held_holding_id     bigint       not null,
	manifestation_id    bigint       not null,
	primary key (org_code, held_holding_id, manifestation_id),
	KEY idx_held_holdings_held_holding_id (held_holding_id),
	KEY idx_held_holdings_manifestation_id (manifestation_id)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- create table bibs_to_holdings (
-- 	org_code            VARCHAR(16)     not null,
-- 	bib_001             VARCHAR(64)     not null,
-- 	holding_001         VARCHAR(64)     not null,
-- 	primary KEY (org_code, bib_001, holding_001),
--     INDEX idx_referenced_bib_001 (bib_001),
--     INDEX idx_referenced_holding_001 (holding_001)
-- ) ENGINE=MyISAM DEFAULT CHARSET=utf8;
