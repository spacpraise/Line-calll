-- Ensure abandoned calls reach a terminal state even when both apps are offline.
create extension if not exists pg_cron with schema pg_catalog;
do $$
declare existing_job bigint;
begin
  select jobid into existing_job from cron.job where jobname='line-call-stale-call-sweeper' limit 1;
  if existing_job is not null then perform cron.unschedule(existing_job); end if;
  perform cron.schedule('line-call-stale-call-sweeper', '* * * * *',
    'select public.sweep_stale_line_calls();');
end $$;
