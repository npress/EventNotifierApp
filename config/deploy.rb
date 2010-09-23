load 'deploy'

# ================================================================
# ROLES
# ================================================================


    role :app, "10.42.42.110"
  
    role :db, "10.42.42.110", {:primary=>true}
  
    role :web, "10.42.42.110"
  

# ================================================================
# VARIABLES
# ================================================================

# Webistrano defaults
  set :webistrano_project, "non_rails_app"
  set :webistrano_stage, "dev"


  set :admin_runner, "npress"

  set :application, "non_rails_app"

  set :deploy_to, "/Users/npress/non_rails_app_from131"

  set :deploy_via, :export

  set :password, "npress123"

  set :rails_env, "development"

  set :repository, "git@github.com:npress/EventNotifierApp.git"

  set :runner, "npress"

  set :scm, :git

  set :use_sudo, true

  set :user, "npress"




# ================================================================
# TEMPLATE TASKS
# ================================================================

        # allocate a pty by default as some systems have problems without
        default_run_options[:pty] = true
      
        # set Net::SSH ssh options through normal variables
        # at the moment only one SSH key is supported as arrays are not
        # parsed correctly by Webistrano::Deployer.type_cast (they end up as strings)
        [:ssh_port, :ssh_keys].each do |ssh_opt|
          if exists? ssh_opt
            logger.important("SSH options: setting #{ssh_opt} to: #{fetch(ssh_opt)}")
            ssh_options[ssh_opt.to_s.gsub(/ssh_/, '').to_sym] = fetch(ssh_opt)
          end
        end
      
         namespace :deploy do
           task :restart, :roles => :app, :except => { :no_release => true } do
             # do nothing
           end

           task :start, :roles => :app, :except => { :no_release => true } do
             # do nothing
           end

           task :stop, :roles => :app, :except => { :no_release => true } do
             # do nothing
           end
         end


# ================================================================
# CUSTOM RECIPES
# ================================================================


  task :cdndeploy do
 puts "Uploading the files from ~releases_storage/client_stage "
 end


