create sequence cindex 
create sequence rindex 

drop table crawl_info_tblc
drop table crawl_page_tblc
drop table crawl_refer_tblc

create table crawl_info_tblc(crawlId varchar(40), siteUrl varchar, creUserId varchar(20), creDate timestamp, options jsonb, indexed boolean, screened boolean)
create index crawl_pk on crawl_info_tblc(crawlId) ;

create table crawl_page_tblc(crawlId varchar(40), cno integer, url varchar, urlhash integer, method varchar(10), scode varchar(4), queryparam varchar, title varchar, screenPath varchar, html text, content text) ;
create index crawl_page_pk on crawl_page_tblc(crawlId, cno) ;
create index crawl_page_urlhash_idx on crawl_page_tblc(crawlId, urlhash) ;

create table crawl_refer_tblc(crawlId varchar(40), rno integer, url varchar, urlhash integer, anchor varchar, referer varchar, referhash integer) ;
create index crawl_refer_pk on crawl_refer_tblc(crawlId, rno) ;
create index crawl_refer_idx on crawl_refer_tblc(crawlId, urlhash) ;


select * from crawl_page_tblc limit 5

alter table crawl_info_tblc add column indexed boolean 
alter table crawl_info_tblc add column screened boolean 

create function util$hash(text) returns int as $$
 select ('x'||substr(md5($1),1,8))::bit(32)::int;
$$ language sql;


select util$hash('1234567890'), util$hash('12345678901')


select * from crawl_info_tblc

truncate table crawl_tblc
truncate table crawl_refer_tblc


select count(*) from crawl_tblc where url like '%.html'


select reverse(substr(reverse(url), 1, 4)), count(*)
from crawl_tblc
group by reverse(substr(reverse(url), 1, 4))




select *
from crawl_old where length(content) > 10
limit 300

select *
from crawl_tblc where length(html) < 100
limit 300



"http://www.meidensha.co.jp/procure/proc_07/index.html"

select substr('1234567', -4, 1)
select reverse(substr(reverse('1234567'), 1, 4))

select count(*) from crawl_refer_tblc  305117

select * from crawl_refer_tblc limit 20



select * from crawl_info_tblc 
select * from crawl_page_tblc limit 100
select * from crawl_refer_tblc limit 100



select x1.siteId, cno, url, urlhash, scode, title, screenPath, html, content
from crawl_page_tblc x1
where 	x1.siteid = '57ecaaf24ccdcfb4d4c6b7a4' and x1.url like '%.html' and x1.scode = '200'



select siteId, rno, url, urlhash, anchor 
from crawl_refer_tblc x1
where 	x1.siteid = '57ecaaf24ccdcfb4d4c6b7a4' and urlhash = util$hash('http://www.meidensha.co.jp/index.html')
	

select CRAWL$htmlPageContentList('57ecaaf24ccdcfb4d4c6b7a4') ;
fetch all from "rcursor" ;



truncate table crawl_info_tblc ;
truncate table crawl_page_tblc ;
truncate table crawl_refer_tblc  ;  
select * from crawl_info_tblc limit 20
select * from crawl_page_tblc limit 20
select * from crawl_refer_tblc limit 20

alter table crawl_page_tblc add column screenPath varchar







CREATE OR REPLACE FUNCTION crawl$createWith(v_crawlid character varying, v_siteurl character varying, v_creuserid character varying, v_option character varying)
RETURNS integer AS
$BODY$
BEGIN
	insert into crawl_info_tblc(crawlId, siteUrl, creUserId, creDate, options)
	values(v_crawlId, v_siteUrl, v_creUserId, now(), v_option::jsonb) ;

	return 1 ;
END $BODY$ LANGUAGE plpgsql ;


CREATE OR REPLACE FUNCTION crawl$addpageWith(v_crawlid character varying, v_url character varying, v_method character varying, v_scode character varying, v_queryparam character varying, v_title character varying, v_html text, v_content text)
RETURNS integer AS
$BODY$
BEGIN
	insert into crawl_page_tblc(crawlId, cno, url, urlhash, method, scode, queryparam, title, html, content)
	values(v_crawlId, nextVal('cindex'), v_url, util$hash(v_url), v_method, v_scode, v_queryparam, v_title, v_html, v_content) ;

	return 1 ;
END $BODY$ LANGUAGE plpgsql ;

CREATE OR REPLACE FUNCTION crawl$tolinkWith(v_crawlid character varying[], v_tourl character varying[], v_anchor character varying[], v_fromurl character varying[])
RETURNS void AS
$BODY$
BEGIN
	insert into crawl_refer_tblc(crawlId, rno, url, urlhash, anchor, referer, referhash)
	Select crawlId, nextVal('rindex'), tourl, util$hash(tourl), anchor, fromurl, util$hash(fromurl)
	From (Select unnest(v_crawlId) crawlId, unnest(v_toUrl) tourl, unnest(v_anchor) anchor, unnest(v_fromUrl) fromurl) b1 ;

END $BODY$ LANGUAGE plpgsql ;


CREATE OR REPLACE FUNCTION crawl$removeWith(v_crawlid character varying)
RETURNS integer AS
$BODY$
BEGIN
	Delete from crawl_info_tblc where crawlId = v_crawlid ;
	Delete from crawl_page_tblc where crawlId = v_crawlid ;
	Delete from crawl_refer_tblc where crawlId = v_crawlid ;

	return 1 ;
END $BODY$ LANGUAGE plpgsql ;


--// make index 
 
CREATE OR REPLACE FUNCTION crawl$htmlContentlistBy(v_crawlid character varying)
RETURNS refcursor AS
$BODY$
DECLARE 
	rtn_cursor refcursor := 'rcursor';
BEGIN
	OPEN rtn_cursor FOR
	select x1.crawlId, cno, url, replace(url, x2.siteUrl, '') relUrl, urlhash, scode, title, screenPath, html, content
	from crawl_page_tblc x1, crawl_info_tblc x2
	where 	x1.crawlId = v_crawlId and x1.url like '%.html' and x1.scode = '200'
		and x1.crawlId = x2.crawlId ;
	
	return rtn_cursor ;
END $BODY$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION crawl$referContentlistBy(v_crawlid character varying, v_tourl character varying)
RETURNS refcursor AS
$BODY$
DECLARE 
	rtn_cursor refcursor := 'rcursor';
BEGIN
	OPEN rtn_cursor FOR
	select crawlId, rno, url, urlhash, anchor 
	from crawl_refer_tblc x1
	where 	x1.crawlId = v_crawlId and urlhash = util$hash(v_tourl) ;
	
	return rtn_cursor ;
END $BODY$ LANGUAGE plpgsql ;

CREATE OR REPLACE FUNCTION crawl$htmlListBy(v_crawlid character varying)
RETURNS refcursor AS
$BODY$
DECLARE 
	rtn_cursor refcursor := 'rcursor';
BEGIN
	OPEN rtn_cursor FOR
	select x1.crawlId, cno, url, replace(url, x2.siteUrl, '') || '.png' path
	from crawl_page_tblc x1, crawl_info_tblc x2
	where 	x1.crawlId = v_crawlId and x1.url like '%.html' and x1.scode = '200' 
		and x1.crawlId = x2.crawlId ;
	
	return rtn_cursor ;
END $BODY$ LANGUAGE plpgsql ;




-- ..

CREATE OR REPLACE FUNCTION crawl$pagelistBy(v_crawlid character varying)
  RETURNS refcursor AS
$BODY$
DECLARE 
	rtn_cursor refcursor := 'rcursor';
BEGIN
	OPEN rtn_cursor FOR
	select x1.crawlId, cno, url, urlhash, replace(url, x2.siteUrl, '') || '.png' path, scode, screenpath
	from crawl_page_tblc x1, crawl_info_tblc x2
	where x1.crawlId = x2.crawlId and x1.crawlId = v_crawlId ;
	
	return rtn_cursor ;
END $BODY$ LANGUAGE plpgsql ;



CREATE OR REPLACE FUNCTION crawl$linklistBy(v_crawlid character varying, v_urlhash character varying)
RETURNS refcursor AS
$BODY$
DECLARE 
	rtn_cursor refcursor := 'rcursor';
BEGIN
	OPEN rtn_cursor FOR
	select 'to' typecd, crawlid, rno, referer as url, anchor, urlhash, referhash
	from crawl_refer_tblc
	where crawlId = v_crawlId and urlhash = v_urlhash::integer
	union all
	select 'from' typecd, crawlid, rno, url, anchor, urlhash, referhash
	from crawl_refer_tblc
	where crawlId = v_crawlId and referhash = v_urlhash::integer ;

	return rtn_cursor ;
END $BODY$ LANGUAGE plpgsql ;








drop function crawl$screenPageWith(v_crawlid character varying[], v_cno integer[], v_spath character varying[])

CREATE OR REPLACE FUNCTION crawl$screenPageWith(v_crawlid character varying[], v_cno integer[], v_spath character varying[])
RETURNS void AS
$BODY$
BEGIN
	update crawl_page_tblc x1 set screenPath = x2.spath
	From (select unnest(v_crawlId) crawlId, unnest(v_cno) cno, unnest(v_spath) spath) x2
	Where x1.crawlId = x2.crawlId and x1.cno = x2.cno ;

END $BODY$ LANGUAGE plpgsql;
  




