import vorpal from 'vorpal'
import { words } from 'lodash'
import { connect } from 'net'
import { Message } from './Message'

export const cli = vorpal()

let username
let server
let mostRecentCommand = ''
// let commandText = ''

cli
  .delimiter(cli.chalk['yellow']('ftd~$'))

cli
  .mode('connect <username> [server] = "localhost"') //AJ added [server] to give the optional command to load a server
  .delimiter(cli.chalk['green']('connected>'))  //https://github.com/chalk/chalk
  .init(function (args, callback) {
    username = args.username
    server = connect({ host: args.server, port: 8080 }, () => {  //aj re
      // placed localhost with args.server to connect to server of choosing
      server.write(new Message({ username, command: 'connect' }).toJSON() + '\n')
      callback()
    })

    server.on('data', (buffer) => {
      this.log(Message.fromJSON(buffer).toString())
    })

    server.on('end', () => {
      cli.exec('exit')
    })
  })



  .action(function (input, callback) {
    let [ command, ...rest ] = words(input, /[^\s ]+/g)  //added lodash expression from https://regex101.com/#javascript so all characters but white spaces work
    let contents = '<' + username + '> ' + '(' + command + ')' + ' ' + rest.join(' ')  //aj edited for printout

    if (command === 'disconnect') {
      server.end(new Message({ username, command }).toJSON() + '\n')


    } else if (command === 'echo') {
      contents = cli.chalk['magenta']('<' + username + '> ' + '(' + command + ') '  + rest.join(' '))
      server.write(new Message({ username, command, contents }).toJSON() + '\n')
      mostRecentCommand = command


    } else if (command === 'broadcast') { //added by AJ
      contents = cli.chalk['blue']('<' + username + '> ' + '(all) ' + '' + rest.join(' '))
      server.write(new Message({ username, command, contents }).toJSON() + '\n') //added by AJ
      mostRecentCommand = command


    } else if (command.charAt(0) ===  '@') { //added by AJ
      let targetUser = command;
      command = '@user'
      contents = cli.chalk['red']('<' + username + '> ' + '(whisper) ' + '' + rest.join(' '))
      server.write(new Message({ username, command, contents, targetUser }).toJSON() + '\n') //added by AJ


    } else if (command === 'users') { //added by AJ
      server.write(new Message({ username, command, contents }).toJSON() + '\n') //added by AJ


    } else if (command !== 'connect' && command !== 'disconnect' && command !== 'echo' && command !== 'broadcast'
         && command.charAt(0) !==  '@' && command !== 'users' && mostRecentCommand === 'broadcast') {
          contents = cli.chalk['blue']('<' + username + '> ' + '(all)' + ' ' + command + ' '  + rest.join(' '))  //aj edited for printout
      server.write(new Message({ username, command: mostRecentCommand, contents: contents }).toJSON() + '\n') //added by AJ


     } else if (command !== 'connect' && command !== 'disconnect' && command !== 'echo' && command !== 'broadcast'
          && command.charAt(0) !==  '@' && command !== 'users' && mostRecentCommand === 'echo') {
            contents = cli.chalk['magenta']('<' + username + '> ' + '(' + mostRecentCommand + ')' + ' ' + command + ' '  + rest.join(' '))  //aj edited for printout
       server.write(new Message({ username, command: mostRecentCommand, contents: contents }).toJSON() + '\n') //added by AJ


    } else {
      this.log(`Command <${command}> was not recognized`)
    }



    callback()
  })
