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
  .mode('connect <username> [server] = "localhost"') //
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


    } else if (command === 'exit') {
        server.end(new Message({ username, command }).toJSON() + '\n')


    } else if (command === 'echo') {
      contents = cli.chalk['magenta'](rest.join(' ')) //'<' + username + '> ' + '(' + command + ') '  +
      server.write(new Message({ username, command, contents }).toJSON() + '\n')
      mostRecentCommand = command


    } else if (command === 'broadcast') {
      contents = cli.chalk['blue'](rest.join(' '))  //'<' + username + '> ' + '(all) ' + '' +
      server.write(new Message({ username, command, contents }).toJSON() + '\n')
      mostRecentCommand = command


    } else if (command.charAt(0) === '@') {

      contents = cli.chalk['red'](rest.join(' ')) //'<' + username + '> ' + '(whisper) ' + '' +
      server.write(new Message({ username, command, contents }).toJSON() + '\n')
      mostRecentCommand = command

    } else if (command === 'users') {
      cli.chalk['blue'](username)
      server.write(new Message({ username, command, contents }).toJSON() + '\n')

    } else if (command !== 'connect' && command !== 'disconnect' && command !== 'echo' && command !== 'broadcast'
         && command.charAt(0) !==  '@' && command !== 'users' && mostRecentCommand === 'broadcast') {
          contents = cli.chalk['blue'](command + ' '  + rest.join(' '))  // '<' + username + '> ' + '(all)' + ' ' +
      server.write(new Message({ username, command: mostRecentCommand, contents: contents }).toJSON() + '\n')


     } else if (command !== 'connect' && command !== 'disconnect' && command !== 'echo' && command !== 'broadcast'
          && command.charAt(0) !==  '@' && command !== 'users' && mostRecentCommand === 'echo') {
            contents = cli.chalk['magenta'](command + ' '  + rest.join(' '))  // '<' + username + '> ' + '(' + mostRecentCommand + ')' + ' ' +
       server.write(new Message({ username, command: mostRecentCommand, contents: contents }).toJSON() + '\n')


     } else if (command !== 'connect' && command !== 'disconnect' && command !== 'echo' && command !== 'broadcast'
          && command.charAt(0) !==  '@' && command !== 'users' && mostRecentCommand.charAt(0) === '@') {
            contents = cli.chalk['red'](command + ' '  + rest.join(' '))  // '<' + username + '> ' + '(' + mostRecentCommand + ')' + ' ' +
       server.write(new Message({ username, command: mostRecentCommand, contents: contents }).toJSON() + '\n')


    } else {
      this.log(`Command <${command}> was not recognized`)
    }



    callback()
  })
