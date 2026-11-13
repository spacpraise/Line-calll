-- Unified 60-second maximum ringing duration across server state and sweeper.
update public.calls set expires_at = created_at + interval '60 seconds' where status = 'ringing';
alter table public.calls alter column expires_at set default (now() + interval '60 seconds');
create or replace function public.sweep_stale_line_calls()
returns integer language plpgsql security definer set search_path=public,pg_temp as $$
declare affected integer := 0; v_rows integer := 0;
begin
  update public.calls set status='missed', ended_at=coalesce(ended_at,now()),
    end_reason='ring_timeout', state_version=state_version+1
  where status='ringing' and coalesce(expires_at,created_at+interval '60 seconds') <= now();
  get diagnostics v_rows = row_count; affected := affected + v_rows;
  update public.calls set status='ended', ended_at=coalesce(ended_at,now()), state_version=state_version+1
  where status='accepted' and heartbeat_enabled=true
    and coalesce(last_activity_at,accepted_at,created_at) < now()-interval '3 minutes';
  get diagnostics v_rows = row_count; affected := affected + v_rows;
  update public.calls set status='ended', ended_at=coalesce(ended_at,now()), state_version=state_version+1
  where status='accepted' and heartbeat_enabled=false
    and coalesce(accepted_at,created_at) < now()-interval '12 hours';
  get diagnostics v_rows = row_count; affected := affected + v_rows;
  return affected;
end $$;
revoke all on function public.sweep_stale_line_calls() from public, anon, authenticated;
grant execute on function public.sweep_stale_line_calls() to service_role;
